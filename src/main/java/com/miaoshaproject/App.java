package com.miaoshaproject;

import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.service.model.UserModel;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello world!
 *
 */

@SpringBootApplication(scanBasePackages ={"com.miaoshaproject"})
@ResponseBody
@MapperScan("com.miaoshaproject.dao")
public class App {

    public static void main( String[] args ) {
        //System.out.println( "Hello World!" );
        SpringApplication.run(App.class,args);
    }
}
