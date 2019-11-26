package com.juju.beautiful.sundry;

/**
 * 无论try块或者catch块中是否包含return语句，都会执行finally块
 * finally中的return会覆盖已有的返回值
 *
 * @author juju
 * @date 2019/04/25
 */
public class FinallyReturn {
    public static void main(String[] args) {
        System.out.println(test2());
        System.out.println(test3());
        System.out.println(test4());
    }

    public static int test2() {
        int i = 1;
        try {
            return i;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            i = 0;
            return i;
        }
    }

    public static int test3() {
        int i = 1;
        try {
            return i;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return 0;
        }
    }

    public static User test4() {
        User user = new User("u1");
        try {
            return user;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            user = new User("u2");
        }
        return null;
    }

    static class User {
        private String name;

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{" + "name='" + name + '\'' + '}';
        }
    }
}
