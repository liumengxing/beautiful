package com.juju.beautiful.thread;

public class PrintLetters {
    // 已经打印得元音个数
    private static int countOne = 0;
    // 已经打印得辅音个数
    private static int countTwo = 0;
    // 打印总次数
    private static int totalCount = 50;
    // 当前获得锁得线程，true打印元音，false打印辅音
    private static volatile boolean flag = true;

    // 元音数组
    private static String[] arrayOne = new String[]{"a", "e", "i", "o", "u"};
    // 辅音数组，只写了部分
    private static String[] arrayTwo = new String[]{"b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "x", "y", "z"};
    // 元音数组长度
    private final static int sizeOne = arrayOne.length;
    // 辅音数组长度
    private final static int sizeTwo = arrayTwo.length;

    public static void main(String[] args) {
        Thread threadOne = new Thread(() -> {
            while (countOne < totalCount) {
                if (flag) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(String.format("元音%s", arrayOne[countOne++ % sizeOne]));
                    flag = false;
                }
            }
        });

        Thread threadTwo = new Thread(() -> {
            while (countTwo < totalCount) {
                if (!flag) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(String.format("辅音%s", arrayTwo[countTwo++ % sizeTwo]));
                    flag = true;
                }
            }
        });

        threadOne.start();
        threadTwo.start();
    }
}
