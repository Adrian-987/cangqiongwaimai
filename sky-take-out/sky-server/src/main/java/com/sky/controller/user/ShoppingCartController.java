package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/user/shoppingCart")
@RestController
public class ShoppingCartController {
    @Autowired
    ShoppingCartService shoppingCartService;

    //添加菜品或者套餐加入购物车或增加已有菜品数量,service层判断
    @PostMapping("/add")
    public Result insertShopping(@RequestBody ShoppingCartDTO shoppingCartDTO){
        shoppingCartService.insertShopping(shoppingCartDTO);
        return Result.success();
    }

    //查询购物车数据
    @GetMapping("/list")
    public Result<List<ShoppingCart>> selectList(){
        log.info("查询购物车");
        List<ShoppingCart> list=shoppingCartService.selectList();
        return Result.success(list);
    }

    //清空购物车
    @DeleteMapping("/clean")
    public Result delectByUserId(){
        shoppingCartService.delectByUserId();
        return Result.success();
    }

    //减少菜品数量
    @PostMapping("/sub")
    public Result updateSub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        shoppingCartService.updateSub(shoppingCartDTO);
        return Result.success();
    }
}
