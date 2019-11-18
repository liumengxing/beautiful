package com.juju.beautiful.sundry;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 实现简单的ArrayList
 * 注意扩容
 *
 * @param <T> 列表存放的数据类型
 * @author juju
 * @date 2019/05/27
 */
public class JujuArrayList<T> implements AutoCloseable {
    /**
     * table最长长度
     */
    private static final int MAX_SIZE = 1024;
    /**
     * 初始table的大小
     */
    private static final int INIT_SIZE = 16;
    /**
     * 扩容因子
     */
    private static final float THREHOLD = 0.75F;

    /**
     * 列表元素
     */
    private Object[] table;
    /**
     * table长度
     */
    private int size;
    /**
     * talbe中有效元素个数
     */
    private int elementCount;

    private JujuArrayList() {
        this.table = new Object[JujuArrayList.INIT_SIZE];
        this.size = JujuArrayList.INIT_SIZE;
        this.elementCount = 0;
    }

    private void add(T element) {
        // 数组中有效元素已经占了75%，扩容
        if (this.elementCount + 1 > this.size * JujuArrayList.THREHOLD) {
            expand();
        }
        this.table[elementCount++] = element;
    }

    private void expand() {
        if (this.size == JujuArrayList.MAX_SIZE) {
            throw new IllegalArgumentException(String.format("size = maxsize[%s],can not expand", JujuArrayList.MAX_SIZE));
        }
        // 可以扩容两倍时，直接扩容，不能扩大两倍时，size = maxsize
        int nextSize = this.size * 2 > JujuArrayList.MAX_SIZE ? JujuArrayList.MAX_SIZE : this.size * 2;
        // 扩容并复制原先的数据
        this.table = Arrays.copyOf(this.table, nextSize);
        // 扩容成功后，各种赋值
        this.size = nextSize;
    }

    public static void main(String[] args) {
        JujuArrayList<Integer> jujuArrayList = new JujuArrayList<>();
        // 测试扩容
        IntStream.range(1, 13).forEach(jujuArrayList::add);
        jujuArrayList.add(2);

        // 测试扩容失败
        try {
            while (true) {
                jujuArrayList.add(2);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void close() {
        this.table = null;
    }
}
