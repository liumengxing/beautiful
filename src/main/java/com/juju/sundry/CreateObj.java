package com.juju.sundry;

import java.io.Serializable;
import java.lang.reflect.Constructor;

/**
 * @author juju
 * @date 2019/04/25
 */
public class CreateObj {
    public static void main(String[] args) throws Exception {

        class Person implements Cloneable, Serializable {
            String firstName;
            String lastName;

            public Person(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            @Override
            public String toString() {
                return this.firstName + "," + this.lastName;
            }

            @Override
            protected Object clone() throws CloneNotSupportedException {
                return new Person(this.firstName + "-clone", this.lastName + "-clone");
            }
        }

        // new
        // 分配内存空间，创建对象；调用构造方法初始化对象
        Person p1 = new Person("jack", "liu");
        System.out.println(p1);

        // 反射
        Class stuClass = Class.forName("Person");
        Constructor constructor = stuClass.getConstructor(String.class);
        Person p2 = (Person) constructor.newInstance("lily", "liu");
        System.out.println(p2);

        // 克隆，本身浅拷贝，再重载的时候，转为深拷贝
        try {
            Person p3 = (Person) p1.clone();
            System.out.println(p3);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        // 反序列化
    }
}
