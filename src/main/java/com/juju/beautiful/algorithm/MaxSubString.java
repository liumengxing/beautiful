package com.juju.beautiful.algorithm;

import org.springframework.util.StringUtils;

/**
 * 最长不重复子串的长度
 */
public class MaxSubString {
    public static void main(String[] args) {
        String orginalStr = "abcbacbb";
        System.out.println(maxSubStrLength(orginalStr));
    }

    private static int maxSubStrLength(String orginalStr) {
        if (StringUtils.isEmpty(orginalStr)) {
            return 0;
        }
        int length = orginalStr.length();
        // 字符串长度至少为2
        int leftCur = 0;
        int rightCur = 0;
        int result = 1;
        while (leftCur <= rightCur && rightCur < length) {
            if (leftCur == rightCur) {
                rightCur++;
                continue;
            }
            // 右指针指向的元素
            String rightChar = orginalStr.substring(rightCur, rightCur + 1);
            String nowSubStr = orginalStr.substring(leftCur, rightCur);
            int rightCharIndex = nowSubStr.indexOf(rightChar);
            // nowSubStr中包含rightChar元素
            if (rightCharIndex > -1) {
                int nowLength = rightCur - leftCur;
                result = Math.max(nowLength, result);
                // leftCur移动到窗口中重复元素的下一位，rightCur右移一位（指向重复元素的下一位）
                leftCur = leftCur + rightCharIndex + 1;
                rightCur++;
            } else {
                rightCur++;
            }
        }

        return result;
    }
}
