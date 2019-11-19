package com.juju.beautiful.thread.smallexamples;

/**
 * 两个线程打印奇偶数
 *
 * @author juju
 * @date 2019/04/08
 */
public class PrintOddEvenNumber {
    private static int totalNumber = 20;

    // region 方法一
    /*
    // 打印1~20
    private static AtomicInteger number = new AtomicInteger(1);
    // oddFlag = true，打印
    private static volatile boolean oddFlag = true;

    public static void main(String[] args) {
        Thread oddThread = new Thread(() -> {
            while (number.get() < totalNumber) {
                if (oddFlag) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println(String.format("odd:%s", number.getAndIncrement()));
                    oddFlag = false;
                }
            }
        });

        Thread evenThread = new Thread(() -> {
            while (number.get() < totalNumber) {
                if (!oddFlag) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(String.format("even:%s", number.getAndIncrement()));
                    oddFlag = true;
                }
            }
        });

        oddThread.start();
        evenThread.start();
    }
    */
    // endregion

    // region 方法二
    private static int number = 0;
    private static Object lock = new Object();

    public static void main(String[] args) {
        Thread oddThread = new Thread(() -> {
            while (number < totalNumber) {
                synchronized (lock) {
                    if (number % 2 == 1) {
                        System.out.println(String.format("odd:%s", number++));
                    } else {
                        lock.notify();
                    }
                }
            }
        });

        Thread evenThread = new Thread(() -> {
            while (number < totalNumber) {
                synchronized (lock) {
                    if (number % 2 == 0) {
                        System.out.println(String.format("even:%s", number++));
                    } else {
                        lock.notify();
                    }
                }
            }
        });

        oddThread.start();
        evenThread.start();
    }
    // endregion
}
