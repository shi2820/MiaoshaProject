package com.miaoshaproject.controller;

import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;

import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


@Controller("User")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*", originPatterns = "*")//DEFAULT_ALLOWED_HEADERS:允许跨域传输所有的header参数，将用于使用token放入header域做session共享的跨域请求
public class UserController extends BaseController{

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;


    /*获取手机验证码接口*/
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST,RequestMethod.GET},consumes ={CONTENT_TYPE_FORMED})  //,consumes ={CONTENT_TYPE_FORMED}
    @ResponseBody
    //用户获取otp短信接口
    public CommonReturnType getOtp(@RequestParam(name = "telephone")String telephone){
        //需要按照一定的规则生成otp验证码
        Random random=new Random();
        int randomInt = random.nextInt(99999);
        randomInt+=10000;
        String otpCode=String.valueOf(randomInt);
        //将otp验证码同对应用户的手机号关联,使用httpSession的方式绑定他的手机号与otpCode
        httpServletRequest.getSession().setAttribute(telephone,otpCode);

        //将otp验证码通过短信通道发送给用户，省略
        System.out.println("telephone="+telephone+"&otpCode="+otpCode);
        System.out.println(CommonReturnType.creat(null).toString());

        return CommonReturnType.creat(null);
    }

    /*用户注册接口*/
    @RequestMapping(value = "/register",method = {RequestMethod.POST,RequestMethod.GET},consumes ={CONTENT_TYPE_FORMED})
    @ResponseBody
    //用户注册接口
    public CommonReturnType register(@RequestParam(name = "telephone")String telephone,
                                     @RequestParam(name = "otpCode")String otpCode,
                                     @RequestParam(name = "name")String name,
                                     @RequestParam(name = "gender")Byte gender,
                                     @RequestParam(name = "age")Integer age,
                                     @RequestParam(name = "password")String password) throws BusinessException, NoSuchAlgorithmException {
        //验证手机号和otpCode是否相符合
        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telephone);
        if (!StringUtils.equals(otpCode,inSessionOtpCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR,"短信验证码不符合");
        }
        //用户注册流程
        UserModel userModel=new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setRegisterMode("byphone");
        userModel.setTelephone(telephone);
        userModel.setEncrptPassword(this.EncodeByMD5(password));; //采用MD5加密

        userService.register(userModel);

        return CommonReturnType.creat(null);
    }

    //自定义的一个MD5加密，原本的MD5Encoder.encode(password.getBytes())不符合要求
    public String EncodeByMD5(String str) throws NoSuchAlgorithmException {
        //确定一个计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Decoder = new BASE64Encoder();
        //加密字符串
        String newStr = base64Decoder.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        return newStr;
    }


    /*用户登录接口*/
    @RequestMapping(value = "/login",method = {RequestMethod.GET,RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telephone")String telephone,
                                  @RequestParam(name = "password")String password) throws BusinessException, NoSuchAlgorithmException {
        //入参校验
        if (StringUtils.isEmpty(telephone)||StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR);
        }

        //用户登录服务，用来校验用户登录是否合法
        UserModel userModel=userService.validateLogin(telephone,this.EncodeByMD5(password));

        //将登录凭证加入到用户登录成功的session内
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
        return CommonReturnType.creat(null);
    }


    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id")Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回给前端
         UserModel userModel=userService.getUserById(id);

         //若获取的对应用户信息不存在
        if (userModel==null){
            userModel.setEncrptPassword("123");
            //throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //将核心领域模型用户对象转化成可供UI使用的viewobject
        UserVO userVO=convertFromModel(userModel);
        return CommonReturnType.creat(userVO);
    }

    private UserVO convertFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }
        UserVO userVO =new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }

}
