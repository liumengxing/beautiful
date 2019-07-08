package com.juju.beautiful.thread;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程求和
 *
 * @author juju
 * @date 2019/03/23
 */
public class MultiThreadSum {
    // 线程总数
    private static final int THREAD_NUM = 10;
    // 每个线程计算的说数字个数
    private static final int PAGE_NUM = 100;
    // 求和结果，每个线程结果都是正数，不涉及aba问题
    private static AtomicLong sum = new AtomicLong(0L);

    public static void main(String[] args) {
        MultiThreadSum multiThreadSum = new MultiThreadSum();
        multiThreadSum.sum();
    }

    private void sum() {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(THREAD_NUM+1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUM);
        for (int i = 0; i < THREAD_NUM; i++) {
            SumThread thread = new SumThread(cyclicBarrier,PAGE_NUM * i + 1, PAGE_NUM * (i + 1));
            executor.submit(thread);
        }

        long safeSum = 0L;
        for (int i = 1; i <= 1000; i++) {
            safeSum += i;
        }
        System.out.println("单线程求和结果：" + safeSum);

        try {
            cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
        System.out.println("多线程求和结果：" + sum.get());
        executor.shutdown();
    }

    static class SumThread implements Runnable {
        CyclicBarrier cyclicBarrier;
        private int start;
        private int end;

        public SumThread(CyclicBarrier cyclicBarrier, int start, int end) {
            this.cyclicBarrier = cyclicBarrier;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            long result = 0L;
            for (int i = start; i <= end; i++) {
                result += i;
            }
            System.out.println(String.format("start = %s, end = %s, sum = %s", start, end, result));
            sum.addAndGet(result);

            try {
                Thread.sleep(10);
                cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

}
