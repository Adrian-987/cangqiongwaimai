package com.sky.constant;

/**
 * Redis key 常量，避免魔法字符串散落在各个类里导致读写不一致
 */
public class RedisKeyConstant {

    /** 店铺营业状态：1 营业，0 打烊 */
    public static final String SHOP_STATUS = "status";
}
