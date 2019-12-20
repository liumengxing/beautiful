package com.juju.beautiful.algorithm;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 射击，用12345表示射中的颜色，0没有射中
 * 给定射击的结果集合，颜色总数
 * 计算：
 * 获取包含所有颜色，连续命中的最短长度
 *
 * @author juju_liu
 */
public class WindowSlip {
    public static void main(String[] args) {
        List<Integer> shootList = Arrays.asList(1, 3, 5, 4, 0, 3, 4, 2, 4, 3, 1, 5, 0, 1, 2);
        int colourCount = 5;
        System.out.println(func(shootList, colourCount));
    }

    /**
     * 计算结果
     *
     * @param shootList   shoot的结果集
     * @param colourCount 颜色总数
     * @return -1没有满足结果的，反之为连续命中切包含所有颜色的最短长度
     */
    private static int func(List<Integer> shootList, int colourCount) {
        // 射中列表空，颜色空，尝试次数 < 颜色种类，尝试列表没有包含所有颜色
        if (shootList.isEmpty() || colourCount <= 0
                || shootList.size() < colourCount || notShootExists(shootList, colourCount)) {
            return -1;
        }

        int shootCount = shootList.size();
        int finalResult = shootCount;
        boolean ifFind = false;
        // 初始化窗口，从0开始，前面colourCount个原色
        int leftCur = 0;
        int rightCur = colourCount - 1;
        // 右指针没有到达列表结束
        // 左指针距离右指针的距离至少有colourCount个元素
        while (rightCur < shootCount && leftCur + colourCount - 1 <= shootCount) {
            // 窗口长度小于颜色种类，移动右指针，保持距离为颜色种类
            if (rightCur - leftCur < colourCount - 1) {
                rightCur = leftCur + colourCount - 1;
                continue;
            }

            // 窗口元素
            List<Integer> checkList = shootList.subList(leftCur, rightCur + 1);
            // 窗口中存在0，将左指针以动到0的下一位元素
            int zeroIndex = zeroIndex(checkList);
            if (zeroIndex > 0) {
                leftCur = leftCur + zeroIndex;
                continue;
            } else {
                // 窗口没有0，但是没有包含所有的元素，将右指针移动一格
                if (notShootExists(checkList, colourCount)) {
                    rightCur++;
                    continue;
                } else {
                    // 窗口没有0，并且包含了所有元素
                    // 找到了一个结果
                    ifFind = true;
                    finalResult = Math.min(rightCur + 1 - leftCur, finalResult);
                    // 窗口宽度 = 颜色个数，并且已经满足条件
                    // 没有比这个更小的结果了，直接退出
                    if (finalResult == colourCount) {
                        break;
                    }
                    // 左指针移动一格，判断有没有更好的结果
                    leftCur++;
                }
            }
        }

        return ifFind ? finalResult : -1;
    }

    /**
     * 传入的List是否存在没有射中的颜色
     *
     * @param checkList   记录list
     * @param colourCount 颜色种类
     * @return true存在没有射中的颜色，false不存在，即全部射中
     */
    private static boolean notShootExists(List<Integer> checkList, int colourCount) {
        return IntStream.range(1, colourCount + 1)
                .anyMatch(colour -> !checkList.contains(colour));
    }

    /**
     * 返回列表中0的位置
     *
     * @param checkList 检查的列表
     * @return 最后一个0是第几个元素，从1开始计数。返回结果为指针的右移距离。返回0表示列表中没有0元素
     */
    private static int zeroIndex(List<Integer> checkList) {
        return checkList.contains(0) ? checkList.lastIndexOf(0) + 1 : 0;
    }
}
