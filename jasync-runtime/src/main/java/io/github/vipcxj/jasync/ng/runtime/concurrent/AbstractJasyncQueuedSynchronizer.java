package io.github.vipcxj.jasync.ng.runtime.concurrent;

import io.github.vipcxj.jasync.ng.spec.JAsyncRoutine;
import io.github.vipcxj.schedule.EventHandle;
import io.github.vipcxj.schedule.Schedule;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class AbstractJasyncQueuedSynchronizer implements java.io.Serializable {

    private static final long serialVersionUID = 5344653732113768596L;

    protected AbstractJasyncQueuedSynchronizer() { }

    // RoutineNode status bits, also used as argument and return values
    static final int WAITING   = 1;          // must be 1
    static final int BUSY      = 2;
    static final int BUSY_TO_REMOVING_THEN_CANCEL = -1;
    static final int BUSY_TO_REMOVING_THEN_RESUME = -2;
    static final int REMOVING_THEN_CANCEL = -3;
    static final int REMOVING_THEN_RESUME = -4;
    static final int REMOVED   = -3;
    static final int COND      = 3;          // in a condition wait

    private static final Schedule SCHEDULE = Schedule.instance();

    static abstract class Node<N extends Node<N>> {
        abstract boolean weakCasNext(N c, N v);
        abstract N getNext();
    }

    static class HeadNode extends Node<RoutineNode> {
        volatile RoutineNode next = TAIL_NEXT;       // visibly nonnull when signallable
        static final AtomicReferenceFieldUpdater<HeadNode, RoutineNode> NEXT = AtomicReferenceFieldUpdater.newUpdater(HeadNode.class, RoutineNode.class, "next");

        @Override
        public RoutineNode getNext() {
            return next;
        }

        @Override
        final boolean weakCasNext(RoutineNode c, RoutineNode v) {  // for cleanQueue
            return NEXT.weakCompareAndSet(this, c, v);
        }
    }

    /** CLH Nodes */
    static class RoutineNode extends Node<RoutineNode> {
        // TAIL_NEXT -> some node -> null
        // When enqueued: TAIL_NEXT
        // When some node enqueued after: some node
        // When dequeued: null
        volatile RoutineNode next;       // visibly nonnull when signallable
        static final AtomicReferenceFieldUpdater<RoutineNode, RoutineNode> NEXT = AtomicReferenceFieldUpdater.newUpdater(RoutineNode.class, RoutineNode.class, "next");
        boolean interruptible;
        Waiter waiter;            // visibly nonnull when enqueued
        volatile EventHandle handle;
        // BUSY <-> WAITING
        // WAITING -> REMOVING_THEN_CANCEL -> REMOVED
        // WAITING -> REMOVING_THEN_RESUME -> REMOVED
        // BUSY -> BUSY_TO_REMOVING_THEN_CANCEL -> REMOVED
        // BUSY -> BUSY_TO_REMOVING_THEN_RESUME -> REMOVED
        volatile int status;
        static final AtomicIntegerFieldUpdater<RoutineNode> STATUS = AtomicIntegerFieldUpdater.newUpdater(RoutineNode.class, "status");

        RoutineNode(Waiter waiter, boolean interruptible) {
            this.waiter = waiter;
            this.interruptible = interruptible;
            this.next = TAIL_NEXT;
        }

        @Override
        public RoutineNode getNext() {
            return next;
        }

        // methods for atomic operations
        @Override
        final boolean weakCasNext(RoutineNode c, RoutineNode v) {  // for cleanQueue
            return NEXT.weakCompareAndSet(this, c, v);
        }
        final boolean casStatus(int pre, int now) {
            return STATUS.compareAndSet(this, pre, now);
        }
        final boolean weakCasStatus(int pre, int now) {
            return STATUS.weakCompareAndSet(this, pre, now);
        }
        final void willRemovedAndCancel() {
            while (true) {
                if (status == WAITING) {
                    if (weakCasStatus(WAITING, REMOVING_THEN_CANCEL)) {
                        return;
                    }
                } else if (status == BUSY) {
                    if (weakCasStatus(BUSY, BUSY_TO_REMOVING_THEN_CANCEL)) {
                        return;
                    }
                } else {
                    break;
                }
            }
        }
        final void willRemovedAndResume() {
            while (true) {
                if (status == WAITING) {
                    if (weakCasStatus(WAITING, REMOVING_THEN_RESUME)) {
                        return;
                    }
                } else if (status == BUSY) {
                    if (weakCasStatus(BUSY, BUSY_TO_REMOVING_THEN_RESUME)) {
                        return;
                    }
                } else {
                    break;
                }
            }
        }

        final void destroy() {
            // before call it, we should use cas to nullify the next.
            assert this.next == null;
            this.status = REMOVED;
            this.waiter = null;
            EventHandle handle = this.handle;
            if (handle != null) {
                handle.remove();
            }
        }

        final boolean isInitOrTail() {
            return this.next == TAIL_NEXT;
        }

        public void setHandle(EventHandle handle) {
            if (this.status == REMOVED) {
                handle.remove();
            } else {
                this.handle = handle;
            }
        }
    }

    private static final RoutineNode TAIL_NEXT = new RoutineNode(null, true);

    // Concrete classes tagged by type
    static final class ExclusiveNode extends RoutineNode {
        ExclusiveNode(Waiter waiter, boolean interruptible) {
            super(waiter, interruptible);
        }
    }
    static final class SharedNode extends RoutineNode {
        SharedNode(Waiter waiter, boolean interruptible) {
            super(waiter, interruptible);
        }
    }

    private static boolean isShared(RoutineNode node) {
        return node instanceof SharedNode;
    }

    static final class ConditionNode extends RoutineNode {
        ConditionNode nextWaiter;            // link to next waiting node

        ConditionNode(Waiter waiter, boolean interruptible) {
            super(waiter, interruptible);
        }
    }

    /**
     * Head of the wait queue, lazily initialized.
     */
    private transient final HeadNode head = new HeadNode();

    /**
     * Tail of the wait queue. After initialization, modified only via casTail.
     */
    private transient Node<RoutineNode> tail = head;

    /**
     * The synchronization state.
     */
    private volatile int state;
    private static final AtomicIntegerFieldUpdater<AbstractJasyncQueuedSynchronizer> STATE = AtomicIntegerFieldUpdater.newUpdater(AbstractJasyncQueuedSynchronizer.class, "state");

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * @return current state value
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        return STATE.compareAndSet(this, expect, update);
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean weakCompareAndSetState(int expect, int update) {
        return STATE.weakCompareAndSet(this, expect, update);
    }

    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(JAsyncRoutine current, int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(JAsyncRoutine current, int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected int tryAcquireShared(JAsyncRoutine current, int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(JAsyncRoutine current, int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Main acquire method, invoked by all exported acquire methods.
     *
     * @param node null unless a reacquiring Condition
     * @param arg the acquire argument
     * @param shared true if shared mode else exclusive
     * @param interruptible true if the waiter can be canceled
     * @param timeout 0 for not timed, positive value for timed in nanosecond.
     */
    @SuppressWarnings("SameParameterValue")
    final void acquire(Waiter waiter, RoutineNode node, int arg, boolean shared, boolean interruptible, long timeout) {
        if (node == null) {
            node = shared ? new SharedNode(waiter, interruptible) : new ExclusiveNode(waiter, interruptible);
            node.waiter = waiter;
        }
        assert node.waiter == waiter;
        assert node.next == TAIL_NEXT;
        node.status = WAITING;
        enqueue(node);
        if (timeout > 0) {
            node.setHandle(SCHEDULE.addEvent(timeout, node::willRemovedAndResume));
        }
        if (node == head.next) {
            signal(arg);
        }
    }

    final void signal(int arg) {
        RoutineNode node = this.head.next;
        while (node != TAIL_NEXT) {
            if (node.status == WAITING) {
                // if fail try again, so just use weak version.
                if (node.weakCasStatus(WAITING, BUSY)) {
                    // status = BUSY | BUSY_TO_REMOVING_THEN_CANCEL | BUSY_TO_REMOVING_THEN_RESUME
                    int acquired;
                    try {
                        acquired = isShared(node) ? tryAcquireShared(node.waiter.getRoutine(), arg) : (tryAcquire(node.waiter.getRoutine(), arg) ? 0 : -1);
                        if (acquired >= 0) {
                            node.waiter.resume(true);
                        }
                    } catch (Throwable t) {
                        node.waiter.reject(t);
                        acquired = 1;
                    }
                    // acquired failed
                    if (acquired < 0) {
                        // still BUSY, so change back to WAITING
                        // we must promise cas failure mean others have changed the status, so here we can't use weak version cas.
                        if (node.status == BUSY && node.casStatus(BUSY, WAITING)) {
                            break;
                        }
                        // not BUSY or some others change the status to BUSY_TO_REMOVING_THEN_CANCEL | BUSY_TO_REMOVING_THEN_RESUME
                        assert node.status == BUSY_TO_REMOVING_THEN_CANCEL || node.status == BUSY_TO_REMOVING_THEN_RESUME;
                        // No others have chance to dequeue, so it is always succeed to dequeue here.
                        if (dequeue(node)) {
                            if (node.status == BUSY_TO_REMOVING_THEN_CANCEL) {
                                node.waiter.cancel();
                            } else if (node.status == BUSY_TO_REMOVING_THEN_RESUME) {
                                node.waiter.resume(false);
                            } else {
                                throw new RuntimeException("This is impossible.");
                            }
                            node.destroy();
                            // Since the current node is dequeued and processed, we should try the next one.
                            node = this.head.next;
                        } else {
                            throw new RuntimeException("This is impossible.");
                        }
                    } else { // acquired succeed. so we should remove the node.
                        // current status is BUSY | BUSY_TO_REMOVING_THEN_CANCEL | BUSY_TO_REMOVING_THEN_RESUME, so no others have chance to dequeue the node.
                        // The dequeue operation should always successful.
                        if (dequeue(node)) {
                            node.destroy();
                            // if subsequent shared-mode acquires might also succeed, we should start next loop to signal.
                            if (acquired != 0) {
                                node = this.head.next;
                            } else {
                                break;
                            }
                        } else {
                            throw new RuntimeException("This is impossible.");
                        }
                    }
                }
            } else if (node.status == REMOVING_THEN_CANCEL) {
                // The status REMOVING_THEN_CANCEL can only be change to REMOVED, so we don't need to try again in a loop.
                // Here using CAS just because of not do the work more than once.
                if (dequeue(node) && node.casStatus(REMOVING_THEN_CANCEL, REMOVED)) {
                    node.waiter.cancel();
                    node.destroy();
                    node = this.head.next;
                } else {
                    break;
                }
            } else if (node.status == REMOVING_THEN_RESUME) {
                // The status REMOVING_THEN_RESUME can only be change to REMOVED, so we don't need to try again in a loop.
                // Here using CAS just because of not do the work more than once.
                if (dequeue(node) && node.casStatus(REMOVING_THEN_RESUME, REMOVED)) {
                    node.waiter.resume(false);
                    node.destroy();
                    node = this.head.next;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    void enqueue(RoutineNode node) {
        assert node.next == TAIL_NEXT;
        while (true) {
            Node<RoutineNode> tail = tryFindTail(this.tail);
            if (tail.getNext() == TAIL_NEXT && tail.weakCasNext(TAIL_NEXT, node)) {
                this.tail = tryFindTail(node);
                break;
            }
        }
    }

    private Node<RoutineNode> tryFindTail(Node<RoutineNode> from) {
        assert from != null;
        Node<RoutineNode> node = from;
        RoutineNode next = from.getNext();
        while (next != null && next != TAIL_NEXT) {
            node = next;
            next = node.getNext();
        }
        return node;
    }

    boolean dequeue(RoutineNode node) {
        assert node != null && node != TAIL_NEXT;
        HeadNode head = this.head;
        RoutineNode n = head.next;
        while (n == node) {
            RoutineNode next = n.next;
            if (next != null && n.weakCasNext(next, null)) {
                head.next = next;
                if (n == tail) {
                    tail = tryFindTail(head);
                }
                return true;
            } else {
                n = head.next;
            }
        }
        return false;
    }


    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     *
     * @param waiter the waiter.
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    public final void acquire(Waiter waiter, int arg) {
        try {
            if (!tryAcquire(waiter.getRoutine(), arg)) {
                acquire(waiter, null, arg, false, false, 0L);
            } else {
                waiter.resume(true);
            }
        } catch (Throwable t) {
            waiter.reject(t);
        }
    }

    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    public final void acquireInterruptibly(Waiter waiter, int arg) {
        try {
            if (!tryAcquire(waiter.getRoutine(), arg)) {
                acquire(waiter, null, arg, false, true, 0L);
            } else {
                waiter.resume(true);
            }
        } catch (Throwable t) {
            waiter.reject(t);
        }
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param waiter the waiter.
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     */
    public final void tryAcquireNanos(Waiter waiter, int arg, long nanosTimeout) {
        if (tryAcquire(waiter.getRoutine(), arg)) {
            waiter.resume(true);
            return;
        }
        if (nanosTimeout <= 0L) {
            waiter.resume(false);
            return;
        }
        acquire(waiter, null, arg, false, true, nanosTimeout);
    }

    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     */
    public final boolean release(JAsyncRoutine current, int arg) {
        if (tryRelease(current, arg)) {
            signal(arg);
            return true;
        }
        return false;
    }

    /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     *
     * @param waiter the waiter.
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     */
    public final void acquireShared(Waiter waiter, int arg) {
        try {
            if (tryAcquireShared(waiter.getRoutine(), arg) < 0) {
                acquire(waiter, null, arg, true, false, 0L);
            } else {
                waiter.resume(true);
            }
        } catch (Throwable t) {
            waiter.reject(t);
        }
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     */
    public final void acquireSharedInterruptibly(Waiter waiter, int arg) {
        try {
            if (tryAcquireShared(waiter.getRoutine(), arg) < 0) {
                acquire(waiter, null, arg, true, true, 0L);
            } else {
                waiter.resume(true);
            }
        } catch (Throwable t) {
            waiter.reject(t);
        }
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     *
     * @param waiter the waiter.
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     */
    public final void tryAcquireSharedNanos(Waiter waiter, int arg, long nanosTimeout) {
            if (tryAcquireShared(waiter.getRoutine(), arg) >= 0) {
                waiter.resume(true);
                return;
            }
            if (nanosTimeout <= 0L) {
                waiter.resume(false);
                return;
            }
            acquire(waiter, null, arg, true, true,
                    System.nanoTime() + nanosTimeout);
    }

    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(JAsyncRoutine current, int arg) {
        if (tryReleaseShared(current, arg)) {
            signal(arg);
            return true;
        }
        return false;
    }

    /**
     * Interpret the waiter.
     * @param waiter the waiter.
     * @return ture if interpret successfully, false if the waiter is not in the queue.
     * it means the waiter has been resumed or not valid.
     */
    public final boolean interpret(Waiter waiter) {
        RoutineNode node = head.getNext();
        while (node != TAIL_NEXT) {
            if (node != null) {
                if (node.waiter == waiter) {
                    node.willRemovedAndCancel();
                    // There should be only one node found. Because the node in the queue means its waiter is waiting
                    // Only when the waiter resumed, the waiter can be enqueued again.
                    return true;
                }
                node = node.getNext();
            }
        }
        return false;
    }


    /**
     * The current owner of exclusive mode synchronization.
     */
    private transient long exclusiveRoutineId;

    /**
     * Sets the thread that currently owns exclusive access.
     * A {@code null} argument indicates that no thread owns access.
     * This method does not otherwise impose any synchronization or
     * {@code volatile} field accesses.
     * @param routine the owner routine
     */
    protected final void setExclusiveRoutine(JAsyncRoutine routine) {
        exclusiveRoutineId = routine != null ? routine.id() : 0L;
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a {@link AbstractQueuedSynchronizer.ConditionObject} method.
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link AbstractQueuedSynchronizer.ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isNotHeldExclusively(JAsyncRoutine current) {
        return current.id() != exclusiveRoutineId;
    }


    /**
     * Returns {@code true} if the apparent first queued waiter, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current waiter is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current waiter
     * is not the first queued waiter.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        RoutineNode s;
        return (s = this.head.next) != TAIL_NEXT && s != null &&
                !isShared(s) && s.status == WAITING;
    }

    /**
     * Queries whether any waiters have been waiting to acquire longer
     * than the current waiter.
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     * <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread()
     *   && hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other waiter will acquire before the current
     * waiter.  Likewise, it is possible for another waiter to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer.html#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     * <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued waiter preceding the
     *         current waiter, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors(JAsyncRoutine current) {
        Waiter first; RoutineNode s;
        return (s = this.head.next) != TAIL_NEXT && s != null &&
                s.status > 0 && s.waiter.getRoutine().id() != current.id();
    }
}
