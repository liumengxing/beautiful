package com.juju.beautiful.sundry;

/**
 * @author juju
 * @date 2019/04/25
 */
public class FinallyReturn {
    public static void main(String[] args) {
        System.out.println(test2());
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
