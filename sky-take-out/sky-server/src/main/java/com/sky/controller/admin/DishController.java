package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("admin/dish")
public class DishController {

    @Autowired
    DishService dishService;

    //添加菜品和口味
    @PostMapping
    public Result insertDish(@RequestBody DishDTO dishDTO){
        log.info("添加菜品{}",dishDTO);
        dishService.insertDish(dishDTO);
        return Result.success();
    }

    //进行分页查询
    @GetMapping("/page")
    public Result selectPage(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询数据{}",dishPageQueryDTO);
        PageResult pageResult=dishService.selectDish(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    //批量删除,在service进行判断能否删除
    @DeleteMapping
    public Result delectByIds(@RequestParam List<Long> ids){
        dishService.delectByIds(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result selectById(@PathVariable Long id){
        DishVO dishVO=dishService.selectById(id);
        return Result.success(dishVO);
    }

    @PutMapping
    public Result updateDish(@RequestBody DishDTO dishDTO){
        log.info("修改数据{}",dishDTO);
        dishService.updateDish(dishDTO);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Dish>> selectBycaIds(Long categoryId){
        List<Dish> list=dishService.selectBycaId(categoryId);
        return Result.success(list);
    }

}
