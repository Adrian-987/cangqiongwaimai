package com.sky.controller.admin;

import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/shop")
public class AdminRedisController {

    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    public Result updateStatus(@PathVariable Integer status){
        redisTemplate.opsForValue().set("status",status);
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> selectStatus(){
        Integer status=(Integer) redisTemplate.opsForValue().get("status");
        return Result.success(status);
    }
}
