package com.juju.sundry;

import java.util.Arrays;

/**
 * @author juju
 * @date 2019/03/31
 */
public class One {
    public static void main(String[] args) {
        One one = new One();
        String[] array = one.func("a", "b", "c");
        Arrays.stream(array).forEach(System.out::println);
    }

    private String[] func(String... strArray) {
        for (int index = 0; index < strArray.length; index++) {
            strArray[index] += "_modify";
        }
        return strArray;
    }
}
