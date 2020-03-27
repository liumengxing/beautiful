package com.juju.beautiful.sundry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @author juju_liu
 * @date 2020/3/27
 */
public class MethodHandler {
    public static void main(String[] args) {
        Object classA = new PrintClassA();
        Object classB = new PrintClassB();

        try {
            getPrintlnMethodHandle(classA).invoke("juju");
            // invokeExact返回object，必须强制转换为println的返回类型
            // 否则NoSuchMethodException
            int a = (int) getPrintlnMethodHandle(classA).invokeExact("juju");
            getPrintlnMethodHandle(classB).invokeExact("juju");
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static MethodHandle getPrintlnMethodHandle(Object receiver) throws Throwable {
        MethodType methodType = MethodType.methodType(int.class, String.class);
        return MethodHandles.lookup().findVirtual(receiver.getClass(), "println", methodType).bindTo(receiver);
    }

    static class PrintClassA {
        public int println(String str) {
            System.out.println("print class a " + str);
            return 1;
        }
    }

    static class PrintClassB {
    }
}
