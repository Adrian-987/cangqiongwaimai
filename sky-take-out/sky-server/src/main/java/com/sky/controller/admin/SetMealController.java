package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetMealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/setmeal")
public class SetMealController {
    @Autowired
    SetMealService setMealService;

    @PostMapping
    public Result insertSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐和套餐关联表:{}",setmealDTO);
        setMealService.insertSetmeal(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> selectPage(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询{}",setmealPageQueryDTO);
        PageResult pageResult=setMealService.selectPage(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    public Result delectByIds(@RequestParam List<Long> ids){
        log.info("根据id删除,特判是否起售:{}",ids);
        setMealService.delectByIds(ids);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    public Result updateStatus(@PathVariable Integer status,Long id){
        log.info("更新起售状态,菜品不可停售");
        setMealService.updateStatus(status,id);
       return Result.success();
    }

    @GetMapping("/{id}")
    public Result<SetmealVO> selectById(@PathVariable Long id){
        log.info("回表查询");
        SetmealVO setmealVO=setMealService.selectByid(id);
        return Result.success(setmealVO);
    }

    @PutMapping
    public Result updateMeal(@RequestBody SetmealDTO setmealDTO){
        log.info("根本数据,先删后改");
        setMealService.updateMeal(setmealDTO);
        return Result.success();
    }
}
