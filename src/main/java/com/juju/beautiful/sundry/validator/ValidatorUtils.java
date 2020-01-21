package com.juju.beautiful.sundry.validator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * 对象验证器
 */
public class ValidatorUtils {
    /**
     * 对象验证方法
     *
     * @param t                 验证的对象
     * @param printAllValidator 是否打印所有验证信息
     * @param <T>               验证对象的类型
     */
    public static <T> void validateParams(T t, boolean printAllValidator) {
        //获得验证器
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        //执行验证
        Set<ConstraintViolation<T>> validators = validator.validate(t);

        if (printAllValidator) {
            for (ConstraintViolation<T> constraintViolation : validators) {
                System.out.println(constraintViolation.getMessage());
            }
        } else {
            //有验证信息，将第一个取出来包装成异常返回
            ConstraintViolation<T> constraintViolation = getFirst(validators, null);
            if (constraintViolation != null) {
                throw new ValidationException(constraintViolation.getMessage());
            }
        }
    }

    /**
     * 方法验证器
     *
     * @param t                 验证的对象
     * @param method            验证的方法
     * @param parameters        验证的参数
     * @param printAllValidator 是否打印所有验证信息
     * @param <T>               验证对象的类型
     */
    public static <T> void validateMethod(T t, Method method, Object[] parameters, boolean printAllValidator) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ExecutableValidator execValidator = validator.forExecutables();

        Set<ConstraintViolation<T>> methodValidators = execValidator.validateParameters(t, method, parameters);

        if (printAllValidator) {
            for (ConstraintViolation<T> constraintViolation : methodValidators) {
                System.out.println(constraintViolation.getMessage());
            }
        } else {
            ConstraintViolation<T> constraintViolation = getFirst(methodValidators, null);
            if (constraintViolation != null) {
                throw new ValidationException(constraintViolation.getMessage());
            }
        }
    }

    private static <T> T getFirst(Set<T> sets, T defaultValue) {
        if (sets.size() > 0) {
            return sets.iterator().next();
        } else {
            return defaultValue;
        }
    }
}
