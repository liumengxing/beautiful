package com.juju.beautiful.jdk8.hibernatevalidator;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.Collection;

@NoArgsConstructor
public class HibernateModel {
    @NotBlank(message = "name不能为空！")
    @Getter
    @Setter
    private String name;

    @NotBlank(message = "strAge不能为空！")
    @Pattern(regexp = "(?:[1-9][0-9]?|1[01][0-9]|120)", message = "strAge格式不正确")
    @Getter
    @Setter
    private String strAge;

    @AssertFalse(message = "assertFalse = false")
    private boolean assertFalse;

    @Pattern(regexp = "[1-2]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])", message = "strBirthday格式不正确")
    @Getter
    @Setter
    private String strBirthday;

    @Min(value = 0, message = "age超出实际最小值")
    @Max(value = 120, message = "age超出实际最大值")
    @Getter
    @Setter
    private int age;

    @Past(message = "birthday不能是未来的时间")
    @Getter
    @Setter
    private LocalDate birthday;

    @NotEmpty(message = "longList不能为空数组")
    @Getter
    @Setter
    private Collection<Long> longList;

    public HibernateModel(String name, String strAge, boolean assertFalse, String strBirthday
            , int age, LocalDate birthday, Collection<Long> longList) {
        this.name = name;
        this.strAge = strAge;
        this.assertFalse = assertFalse;
        this.strBirthday = strBirthday;
        this.age = age;
        this.birthday = birthday;
        this.longList = longList;
    }

    public void integerEquals70(@Max(value = 70, message = "函数integerEquals70,参数integer不能大于70")
                                @Min(value = 70, message = "函数integerEquals70,参数integer不能小于70") int integer) {
    }
}

