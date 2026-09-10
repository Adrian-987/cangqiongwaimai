package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    User selectByOpenid(String openId);

    void insertUser(User user);

    User getById(Long userId);

    Integer selectUser(Map map);
}
