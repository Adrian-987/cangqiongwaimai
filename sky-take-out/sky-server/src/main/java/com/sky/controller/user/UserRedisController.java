package com.sky.controller.user;

import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user/shop")
public class UserRedisController {

    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping("/status")
    public Result<Integer> selectStatus(){
        Integer status=(Integer) redisTemplate.opsForValue().get("status");
        return Result.success(status);
    }

}
