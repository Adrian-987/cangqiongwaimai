package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void insertDish(DishDTO dishDTO);

    PageResult selectDish(DishPageQueryDTO dishPageQueryDTO);

    void delectByIds(List<Long> ids);

    DishVO selectById(Long id);

    void updateDish(DishDTO dishDTO);

    List<Dish> selectBycaId(Long categoryId);
}
