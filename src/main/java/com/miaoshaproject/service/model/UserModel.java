package com.miaoshaproject.service.model;


/*
* 这是用户完整模型
* 由于初始时用户密码和其他属性分为了两个模型，但需要综合处理时（比如用户注册等），需要一个完整的带密码的模型*/

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UserModel {
    private Integer id;

    @NotBlank(message = "用户名不能为空")
    private String name;

    @NotNull(message = "性别不能不填")
    private Byte gender;

    @NotNull(message = "年龄不能不填写")
    @Min(value = 0,message = "年龄必须大于0")
    @Max(value = 150,message = "年龄必须小于150")
    private Integer age;

    @NotBlank(message = "手机号不能为空")
    private String telephone;
    private String registerMode;
    private String thirdPartyId;

    @NotBlank(message = "密码不能为空")
    private String encrptPassword;

    public String getEncrptPassword() {
        return encrptPassword;
    }

    public void setEncrptPassword(String encrptPassword) {
        this.encrptPassword = encrptPassword;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getRegisterMode() {
        return registerMode;
    }

    public void setRegisterMode(String registerMode) {
        this.registerMode = registerMode;
    }

    public String getThirdPartyId() {
        return thirdPartyId;
    }

    public void setThirdPartyId(String thirdPartyId) {
        this.thirdPartyId = thirdPartyId;
    }
}
