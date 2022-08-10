package io.github.vipcxj.jasync.ng.runtime.concurrent;

import io.github.vipcxj.jasync.ng.runtime.promise.LockPromise;
import io.github.vipcxj.jasync.ng.spec.JAsyncLock;
import io.github.vipcxj.jasync.ng.spec.JAsyncReadWriteLock;
import io.github.vipcxj.jasync.ng.spec.JAsyncRoutine;
import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.concurrent.TimeUnit;

public class ReentrantReadWriteLock implements JAsyncReadWriteLock, java.io.Serializable {

    private static final long serialVersionUID = -7486277905008989363L;
    private final Sync sync;
    private final JAsyncLock readLock;
    private final JAsyncLock writeLock;

    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readLock = new ReadLock();
        writeLock = new WriteLock();
    }

    public ReentrantReadWriteLock() {
        this(false);
    }

    @Override
    public JAsyncLock readLock() {
        return readLock;
    }

    @Override
    public JAsyncLock writeLock() {
        return writeLock;
    }

    abstract public static class Sync extends AbstractJasyncQueuedSynchronizer {

        private static final long serialVersionUID = 587300015244198651L;

        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */

        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;


        /**
         * firstReader is the first waiter to have acquired the read lock.
         * firstReaderHoldCount is firstReader's hold count.
         *
         * <p>More precisely, firstReader is the unique waiter that last
         * changed the shared count from 0 to 1, and has not released the
         * read lock since then; null if there is no such waiter.
         *
         * <p>Cannot cause garbage retention unless the waiter terminated
         * without relinquishing its read locks, since tryReleaseShared
         * sets it to null.
         *
         * <p>Accessed via a benign data race; relies on the memory
         * model's out-of-thin-air guarantees for references.
         *
         * <p>This allows tracking of read holds for uncontended read
         * locks to be very cheap.
         */
        private transient JAsyncRoutine firstReader;
        private transient int firstReaderHoldCount;

        /** Returns the number of shared holds represented in count. */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** Returns the number of exclusive holds represented in count. */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }


        /**
         * Returns true if the current thread, when trying to acquire
         * the read lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        abstract boolean readerShouldBlock(JAsyncRoutine current);

        /**
         * Returns true if the current thread, when trying to acquire
         * the write lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        abstract boolean writerShouldBlock(JAsyncRoutine current);

        /*
         * Note that tryRelease and tryAcquire can be called by
         * Conditions. So it is possible that their arguments contain
         * both read and write holds that are all released during a
         * condition wait and re-established in tryAcquire.
         */
        protected final boolean tryRelease(JAsyncRoutine current, int releases) {
            if (isNotHeldExclusively(current))
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free)
                setExclusiveRoutine(null);
            setState(nextc);
            return free;
        }

        protected final boolean tryAcquire(JAsyncRoutine current, int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || isNotHeldExclusively(current))
                    return false;
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // Reentrant acquire
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock(current) ||
                    !compareAndSetState(c, c + acquires))
                return false;
            setExclusiveRoutine(current);
            return true;
        }

        protected final boolean tryReleaseShared(JAsyncRoutine current, int unused) {
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                int count = current.getSharedLockCount();
                if (count <= 0)
                    throw unmatchedUnlockException();
                current.decSharedLockCount();
            }
            for (;;) {
                int c = getState();
                int nextC = c - SHARED_UNIT;
                if (weakCompareAndSetState(c, nextC))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextC == 0;
            }
        }

        private static IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                    "attempt to unlock read lock, not locked by current thread");
        }

        protected final int tryAcquireShared(JAsyncRoutine current, int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            int c = getState();
            if (exclusiveCount(c) != 0 && isNotHeldExclusively(current))
                return -1;
            int r = sharedCount(c);
            if (!readerShouldBlock(current) &&
                    r < MAX_COUNT &&
                    compareAndSetState(c, c + SHARED_UNIT)) {
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    current.incSharedLockCount();
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }

        /**
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant reads not dealt with in tryAcquireShared.
         */
        final int fullTryAcquireShared(JAsyncRoutine current) {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0) {
                    if (isNotHeldExclusively(current))
                        return -1;
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                } else if (readerShouldBlock(current)) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        assert firstReaderHoldCount > 0;
                    } else {
                        if (current.getSharedLockCount() == 0)
                            return -1;
                    }
                }
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        current.incSharedLockCount();
                    }
                    return 1;
                }
            }
        }

        /**
         * Performs tryLock for write, enabling barging in both modes.
         * This is identical in effect to tryAcquire except for lack
         * of calls to writerShouldBlock.
         */
        final boolean tryWriteLock(JAsyncRoutine current) {
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || isNotHeldExclusively(current))
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveRoutine(current);
            return true;
        }

        /**
         * Performs tryLock for read, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock(JAsyncRoutine current) {
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 && isNotHeldExclusively(current))
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        current.incSharedLockCount();
                    }
                    return true;
                }
            }
        }
    }

    /**
     * Nonfair version of Sync
     */
    static final class NonfairSync extends Sync {

        private static final long serialVersionUID = 4811297190483727472L;

        final boolean writerShouldBlock(JAsyncRoutine current) {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock(JAsyncRoutine current) {
            /* As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * Fair version of Sync
     */
    static final class FairSync extends Sync {

        private static final long serialVersionUID = 1424468862672726825L;

        final boolean writerShouldBlock(JAsyncRoutine current) {
            return hasQueuedPredecessors(current);
        }
        final boolean readerShouldBlock(JAsyncRoutine current) {
            return hasQueuedPredecessors(current);
        }
    }

    final class ReadLock implements JAsyncLock {

        @Override
        public JPromise<Boolean> lock() {
            return new LockPromise(sync, true, false, 0L);
        }

        @Override
        public JPromise<Boolean> lockInterruptibly() {
            return new LockPromise(sync, true, true, 0L);
        }

        @Override
        public boolean tryLock(JAsyncRoutine routine) {
            return sync.tryReadLock(routine);
        }

        @Override
        public JPromise<Boolean> tryLock(long time, TimeUnit unit) {
            return new LockPromise(sync, true, true, unit.toNanos(time));
        }

        @Override
        public void unlock(JAsyncRoutine routine) {
            sync.releaseShared(routine, 1);
        }
    }

    final class WriteLock implements JAsyncLock {

        @Override
        public JPromise<Boolean> lock() {
            return new LockPromise(sync, false, false, 0L);
        }

        @Override
        public JPromise<Boolean> lockInterruptibly() {
            return new LockPromise(sync, false, true, 0L);
        }

        @Override
        public boolean tryLock(JAsyncRoutine routine) {
            return sync.tryWriteLock(routine);
        }

        @Override
        public JPromise<Boolean> tryLock(long time, TimeUnit unit) {
            return new LockPromise(sync, false, true, unit.toNanos(time));
        }

        @Override
        public void unlock(JAsyncRoutine routine) {
            sync.release(routine, 1);
        }
    }
}
