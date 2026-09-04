package com.sky.service;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetMealService {
    void insertSetmeal(SetmealDTO setmealDTO);

    PageResult selectPage(SetmealPageQueryDTO setmealPageQueryDTO);

    void delectByIds(List<Long> ids);

    void updateStatus(Integer status, Long id);

    SetmealVO selectByid(Long id);

    void updateMeal(SetmealDTO setmealDTO);

    /**
     * 条件查询
     * @param setmeal
     * @return
     */

    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}
