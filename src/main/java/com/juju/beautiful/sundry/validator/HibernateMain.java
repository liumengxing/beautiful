package com.juju.beautiful.sundry.validator;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HibernateMain {
    public static void main(String[] args) {
        HibernateMain main = new HibernateMain();
        main.testNotFit();
        main.testFit();
        main.testLombok();
        main.testIntegerEquals70();
    }

    private void testNotFit() {
        try {
            HibernateModel notFitModel = new HibernateModel(null, "122", true, "strBirthday"
                    , -1, LocalDate.parse("3000-02-19"), new LinkedList<>());

            ValidatorUtils.validateParams(notFitModel, true);
        } catch (Exception ex) {
            System.out.println("exception:" + ex.getMessage());
        }
    }

    private void testFit() {
        try {
            List<Long> longList = new ArrayList<>();
            longList.add(1L);
            HibernateModel fitModel = new HibernateModel("name", "120", false, "1991-03-19"
                    , 20, LocalDate.parse("1991-02-19"), longList);

            ValidatorUtils.validateParams(fitModel, true);
        } catch (Exception ex) {
            System.out.println("exception:" + ex.getMessage());
        }
    }

    private void testLombok() {
        HibernateModel noArgsConstructorModel = new HibernateModel();
        noArgsConstructorModel.setName("name");
        System.out.println(noArgsConstructorModel.getName());
    }

    private void testIntegerEquals70() {
        Method method = null;
        try {
            method = HibernateModel.class.getMethod("integerEquals70", int.class);
        } catch (NoSuchMethodException ex) {
            System.out.println(ex.getMessage());
        }

        try {
            Object[] params = {71};
            ValidatorUtils.validateMethod(new HibernateModel(), method, params, false);
        } catch (Exception ex) {
            System.out.println("exception:" + ex.getMessage());
        }
    }
}
