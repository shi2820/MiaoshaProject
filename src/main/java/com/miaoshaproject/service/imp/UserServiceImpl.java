package com.miaoshaproject.service.imp;

import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dao.UserPasswordDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.dataobject.UserPasswordDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;


    @Override
    public UserModel getUserById(Integer id) {
        //调用userdomapper获取到对应的用户dataobject
        UserDO userDO=userDOMapper.selectByPrimaryKey(id);

        if (userDO==null){
            return null;
        }
        //通过用户id获取对应的用户加密密码信息
        UserPasswordDO userPasswordDO=userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel=convertFromDataObject(userDO,userPasswordDO);
        return userModel;
    }

    //用户注册流程
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        if (userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR);
        }

      /*  //这里更改校验方式为使用validator注释的方式
        原方式为:
        if (StringUtils.isEmpty(userModel.getName())
                ||userModel.getAge()==null
                ||userModel.getGender()==null
                ||StringUtils.isEmpty(userModel.getTelephone())){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR);
        }*/

        ValidationResult result = validator.validationResult(userModel);
        if (result.isHasError()){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR,result.getErrMsg());
        }

        //实现model->dataobject方法(用户注册模型转换到dao层，即写入数据库)
        UserDO userDO=convertFromModel(userModel);
        try {
            userDOMapper.insertSelective(userDO); //修改数据库中的telephone为唯一字段，防止出现重复注册，并用try catch获取异常并抛出自定义的异常，给用户较好的体验
        }catch (DuplicateKeyException ex){
            throw new BusinessException(EmBusinessError.PARAMETER_VLIDATION_ERROR,"手机号已注册");
        }

        userModel.setId(userDO.getId());

        UserPasswordDO userPasswordDO=new UserPasswordDO();
        userPasswordDO=convertPassWordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);
        return;
    }


    //验证用户登录信息是否匹配
    @Override
    public UserModel validateLogin(String telephone, String encryptPassword) throws BusinessException {
        //通过用户的手机号获取用户的信息
        UserDO userDO = userDOMapper.selectByTelephone(telephone);
        if (userDO==null){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPasswordDO userPasswordDO=userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel=convertFromDataObject(userDO,userPasswordDO);

        //比对用户信息内加密的密码是否和传输进来的密码相匹配
        if (!StringUtils.equals(encryptPassword,userModel.getEncrptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;
    }

    private UserPasswordDO convertPassWordFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }
        UserPasswordDO userPasswordDO=new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }

    private UserDO convertFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }
        UserDO userDO=new UserDO();
        BeanUtils.copyProperties(userModel,userDO);
        return userDO;
    }

    private UserModel convertFromDataObject(UserDO userDo, UserPasswordDO userPasswordDO){
        if (userDo==null){
            return null;
        }
        UserModel userModel=new UserModel();
        BeanUtils.copyProperties(userDo,userModel);
        if(userPasswordDO!=null){
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }
        return userModel;
    }
}
