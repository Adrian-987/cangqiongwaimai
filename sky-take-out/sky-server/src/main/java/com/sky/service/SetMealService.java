package com.sky.service;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetMealService {
    void insertSetmeal(SetmealDTO setmealDTO);

    PageResult selectPage(SetmealPageQueryDTO setmealPageQueryDTO);

    void delectByIds(List<Long> ids);

    void updateStatus(Integer status, Long id);

    SetmealVO selectByid(Long id);

    void updateMeal(SetmealDTO setmealDTO);
}
