package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    WeChatProperties weChatProperties;

    public static final  String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session";

    @Override
    public User login(UserLoginDTO userLoginDTO) throws IOException {
        String openId=GetOpenid(userLoginDTO.getCode());
        if(openId==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        User user=userMapper.selectByOpenid(openId);
        //创建数据库数据
        if(user==null){
            user=User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insertUser(user);
        }
        return user;
    }
    //单独抽取方法,作为客户端发出请求,校验得到id,错误会具体抛出异常
    private String GetOpenid(String code) throws IOException {
        Map<String,String> map=new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json=HttpClientUtil.doGet(WX_LOGIN,map);
        JSONObject jsonObject = JSON.parseObject(json);
        String openId=jsonObject.getString("openid");
        if (openId == null || openId.trim().isEmpty()) {
            String errcode = jsonObject.getString("errcode");
            String errmsg = jsonObject.getString("errmsg");
            String reason = errmsg == null || errmsg.trim().isEmpty()
                    ? "微信接口未返回openid"
                    : errmsg;
            if (errcode != null && !errcode.trim().isEmpty()) {
                reason = "微信登录失败（错误码 " + errcode + "）：" + reason;
            }
            throw new LoginFailedException(reason);
        }
        return openId;
    }
}
