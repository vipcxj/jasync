package io.github.vipcxj.jasync.ng.runtime.utils;

import org.jctools.queues.MpmcUnboundedXaddArrayQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UnPaddedLockFreeArrayQueueTest {

    @Test
    public void testOffer() {
        UnPaddedLockFreeArrayQueue0<Integer> queue = new UnPaddedLockFreeArrayQueue0<>(2);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        Assertions.assertEquals(1, queue.get(0));
        Assertions.assertEquals(2, queue.get(1));
        Assertions.assertEquals(3, queue.get(2));
    }

    @Test
    public void testPoll() {
        UnPaddedLockFreeArrayQueue0<Integer> queue = new UnPaddedLockFreeArrayQueue0<>(2);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        Assertions.assertEquals(1, queue.poll());
        Assertions.assertEquals(2, queue.poll());
        Assertions.assertEquals(3, queue.poll());
        Assertions.assertNull(queue.poll());
    }

    @Test
    public void testPeek() {
        UnPaddedLockFreeArrayQueue0<Integer> queue = new UnPaddedLockFreeArrayQueue0<>(2);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        Assertions.assertEquals(1, queue.peek());
        queue.poll();
        Assertions.assertEquals(2, queue.peek());
        queue.poll();
        Assertions.assertEquals(3, queue.peek());
        queue.poll();
        Assertions.assertNull(queue.peek());
    }

    @Test
    public void testMixed() {
        UnPaddedLockFreeArrayQueue0<Integer> queue = new UnPaddedLockFreeArrayQueue0<>(2);
        Assertions.assertNull(queue.peek());
        Assertions.assertNull(queue.poll());
        Assertions.assertTrue(queue.offer(1));
        Assertions.assertTrue(queue.offer(2));
        Assertions.assertTrue(queue.offer(3));
        Assertions.assertEquals(1, queue.poll());
        Assertions.assertTrue(queue.offer(4));
        Assertions.assertTrue(queue.offer(5));
        Assertions.assertEquals(2, queue.poll());
        Assertions.assertEquals(3, queue.poll());
        Assertions.assertEquals(4, queue.peek());
        Assertions.assertEquals(4, queue.poll());
        Assertions.assertTrue(queue.offer(6));
        Assertions.assertEquals(5, queue.poll());
        Assertions.assertEquals(6, queue.peek());
        Assertions.assertEquals(6, queue.poll());
        Assertions.assertNull(queue.peek());
        Assertions.assertNull(queue.poll());
    }

    private Runnable multiThreadMixedRunner(Queue<Integer> queue, int loop, int index, AtomicInteger num, AtomicInteger sum, AtomicLong timerHolder) {
        return () -> {
            boolean offer = (index & 1) != 0;
            long timer = startTimer();
            if (offer) {
                for (int j = 0; j < loop;) {
                    for (int i = 0; i < 10; ++i) {
                        Assertions.assertTrue(queue.offer(num.getAndIncrement()));
                    }
                    for (int i = 0; i < 10; ++i) {
                        Integer value = queue.poll();
                        if (value != null) {
                            sum.addAndGet(value);
                        }
                    }
                    j += 10;
                }
            } else {
                for (int j = 0; j < loop;) {
                    for (int i = 0; i < 10; ++i) {
                        Integer value = queue.poll();
                        if (value != null) {
                            sum.addAndGet(value);
                        }
                    }
                    for (int i = 0; i < 10; ++i) {
                        Assertions.assertTrue(queue.offer(num.getAndIncrement()));
                    }
                    j += 10;
                }
            }
            long used = stopTimer(timer, null);
            if (timerHolder != null) {
                timerHolder.addAndGet(used);
            }
        };
    }

    private Runnable multiThreadOfferRunner(Queue<Integer> queue, int loop, AtomicInteger num, AtomicLong timerHolder) {
        return () -> {
            long timer = startTimer();
            for (int j = 0; j < loop; ++j) {
                Assertions.assertTrue(queue.offer(num.getAndIncrement()));
            }
            long used = stopTimer(timer, null);
            if (timerHolder != null) {
                timerHolder.addAndGet(used);
            }
        };
    }

    private Runnable multiThreadPollRunner(Queue<Integer> queue, int loop, AtomicInteger sum, AtomicLong timerHolder) {
        return () -> {
            long timer = startTimer();
            for (int j = 0; j < loop; ++j) {
                Integer v = queue.poll();
                Assertions.assertNotNull(v);
                sum.addAndGet(v);
            }
            long used = stopTimer(timer, null);
            if (timerHolder != null) {
                timerHolder.addAndGet(used);
            }
        };
    }

    @Test
    public void testMultiThread() throws InterruptedException {
        UnPaddedLockFreeArrayQueue0<Integer> queue = new UnPaddedLockFreeArrayQueue0<>(2);
        AtomicInteger num = new AtomicInteger(0);
        AtomicInteger sum = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 1000; ++i) {
            Thread thread = new Thread(multiThreadMixedRunner(queue, 3000, i, num, sum, null));
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        Integer v;
        while ((v = queue.poll()) != null) {
            sum.addAndGet(v);
        }
        if ((num.get() & 1) == 0) {
            Assertions.assertEquals(num.get() / 2 * (num.get() - 1), sum.get());
        } else {
            Assertions.assertEquals((num.get() - 1) / 2 * num.get(), sum.get());
        }
    }

    private long startTimer() {
        return System.nanoTime();
    }

    private void showTimer(long timer, String tag) {
        System.out.println("[" + tag + "]: " + timer / 1000 / 1000 + "ms");
    }

    private long stopTimer(long timer, String tag) {
        timer = System.nanoTime() - timer;
        if (tag != null) {
            showTimer(timer, tag);
        }
        return timer;
    }

    @Test
    public void testSingleThreadPerformance() {
        //noinspection MismatchedQueryAndUpdateOfCollection
        Deque<Integer> queue1 = new ArrayDeque<>(2);
        //noinspection MismatchedQueryAndUpdateOfCollection
        UnPaddedLockFreeArrayQueue1<Integer> queue0 = new UnPaddedLockFreeArrayQueue1<>(2);
        //noinspection MismatchedQueryAndUpdateOfCollection
        ComparedArrayQueue<Integer> queue2 = new ComparedArrayQueue<>(2);
        //noinspection MismatchedQueryAndUpdateOfCollection
        PaddedLockFreeArrayQueue<Integer> queue3 = new PaddedLockFreeArrayQueue<>(2);
        long timer;
        final int outLoop = 1000;
        final int innerLoop = 10000;
        timer = startTimer();
        for (int i = 0; i < outLoop; ++i) {
            for (int j = 0; j < innerLoop; ++j) {
                queue1.offer(i);
            }
            for (int j = 0; j < innerLoop; ++j) {
                queue1.poll();
            }
        }
        stopTimer(timer, "Single Thread / ArrayDeque");
        timer = startTimer();
        for (int i = 0; i < outLoop; ++i) {
            for (int j = 0; j < innerLoop; ++j) {
                queue0.offer(i);
            }
            for (int j = 0; j < innerLoop; ++j) {
                queue0.poll();
            }
        }
        stopTimer(timer, "Single Thread / UnPaddedLockFreeArrayQueue0");
        timer = startTimer();
        for (int i = 0; i < outLoop; ++i) {
            for (int j = 0; j < innerLoop; ++j) {
                queue2.offer(i);
            }
            for (int j = 0; j < innerLoop; ++j) {
                queue2.poll();
            }
        }
        stopTimer(timer, "Single Thread / ComparedArrayQueue");
        timer = startTimer();
        for (int i = 0; i < outLoop; ++i) {
            for (int j = 0; j < innerLoop; ++j) {
                queue3.offer(i);
            }
            for (int j = 0; j < innerLoop; ++j) {
                queue3.poll();
            }
        }
        stopTimer(timer, "Single Thread / PaddedLockFreeArrayQueue");
    }

    @SuppressWarnings("SameParameterValue")
    private void doOfferAndPollMultiThreadPerformance(Queue<Integer> queue, int loop, String tag) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        AtomicInteger num = new AtomicInteger();
        AtomicInteger sum = new AtomicInteger();
        AtomicLong timerHolder = new AtomicLong();
        for (int i = 0; i < 1000; ++i) {
            threads.add(new Thread(multiThreadOfferRunner(queue, loop, num, timerHolder)));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        showTimer(timerHolder.get(), "Multi Thread Offer / " + tag);
        threads.clear();
        for (int i = 0; i < 1000; ++i) {
            threads.add(new Thread(multiThreadPollRunner(queue, loop, sum, timerHolder)));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        showTimer(timerHolder.get(), "Multi Thread Poll / " + tag);
        System.out.println("[Multi Thread Sum / " + tag + "]: " + sum.get());
    }

    @Test
    public void testOfferAndPollMultiThreadPerformance() throws InterruptedException {
        int size = 1000 * 100;
        System.gc();
        doOfferAndPollMultiThreadPerformance(new UnPaddedLockFreeArrayQueue0<>(size), 1000, "UnPaddedLockFreeArrayQueue0");
        System.gc();
        doOfferAndPollMultiThreadPerformance(new UnPaddedLockFreeArrayQueue1<>(size), 1000, "UnPaddedLockFreeArrayQueue1");
        System.gc();
        doOfferAndPollMultiThreadPerformance(new PaddedLockFreeArrayQueue<>(size), 1000, "PaddedLockFreeArrayQueue");
        System.gc();
        doOfferAndPollMultiThreadPerformance(new ComparedArrayQueue<>(size), 1000, "ComparedArrayQueue");
        System.gc();
        doOfferAndPollMultiThreadPerformance(new MpmcUnboundedXaddArrayQueue<>(256), 1000, "MpmcUnboundedXaddArrayQueue");
    }

    private void doMixedMultiThreadPerformance(Queue<Integer> queue, String tag) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        AtomicInteger num = new AtomicInteger();
        AtomicInteger sum = new AtomicInteger();
        AtomicLong timerHolder = new AtomicLong();
        for (int i = 0; i < 1000; ++i) {
            threads.add(new Thread(multiThreadMixedRunner(queue, 5000, i, num, sum, timerHolder)));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        Integer v;
        while ((v = queue.poll()) != null) {
            sum.addAndGet(v);
        }
        System.out.println("[Multi Thread / " + tag + "]: sum " + sum.get());
        showTimer(timerHolder.get(), "Multi Thread / " + tag);
    }

    @Test
    public void testMixedMultiThreadPerformance() throws InterruptedException {
        doMixedMultiThreadPerformance(new MpmcUnboundedXaddArrayQueue<>(64), "MpmcUnboundedXaddArrayQueue");
        doMixedMultiThreadPerformance(new UnPaddedLockFreeArrayQueue0<>(2), "UnPaddedLockFreeArrayQueue1");
        doMixedMultiThreadPerformance(new UnPaddedLockFreeArrayQueue0<>(2), "UnPaddedLockFreeArrayQueue0");
        doMixedMultiThreadPerformance(new PaddedLockFreeArrayQueue<>(2), "PaddedLockFreeArrayQueue");
        doMixedMultiThreadPerformance(new ComparedArrayQueue<>(2), "ComparedArrayQueue");
    }

    static class ComparedArrayQueue<T> extends AbstractQueue<T> implements AutoCapacityQueue<T> {

        private int head;
        private int tail;
        private Object[] elements;

        ComparedArrayQueue(int initCapacity) {
            this.head = this.tail = 0;
            this.elements = new Object[CommonUtils.normalCapacity(initCapacity)];
        }

        @Override
        public Iterator<T> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return tail - head;
        }

        @Override
        public synchronized boolean offer(T t) {
            if (tail - head == elements.length) {
                Object[] newElements = new Object[elements.length << 1];
                System.arraycopy(elements, 0, newElements, 0, elements.length);
                System.arraycopy(elements, 0, newElements, elements.length, elements.length);
                elements = newElements;
            }
            elements[tail++ & (elements.length - 1)] = t;
            return true;
        }

        @Override
        public synchronized T poll() {
            if (tail == head) {
                return null;
            }
            //noinspection unchecked
            return (T) elements[head++ & (elements.length - 1)];
        }

        @Override
        public synchronized T peek() {
            if (tail == head) {
                return null;
            }
            //noinspection unchecked
            return (T) elements[head & (elements.length - 1)];
        }

        public int capacity() {
            return elements.length;
        }

        @Override
        public synchronized void clear(int maxCapacity) {
            if (elements.length > maxCapacity) {
                elements = new Object[maxCapacity];
            }
            head = tail = 0;
        }
    }
}
