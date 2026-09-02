package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealFishMapper {
    void insert(List<SetmealDish> list);

    void delectBySetmealId(Long setmealId);

    List<SetmealDish> selectBySetmealId(Long id);
}
