package com.sky.constant;

import org.springframework.util.DigestUtils;

/**
 * 密码常量
 */
public class PasswordConstant {

    public static final String DEFAULT_PASSWORD = "123456";
    public static final String DEFAULT_MD5PASSWORD= DigestUtils.md5DigestAsHex("123456".getBytes());

}
