package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    // dish_flavor 表没有 create_time 等公共审计字段，不加 @AutoFill
    void insertFlavor(List<DishFlavor> flavors);

    @Delete("delete from dish_flavor where dish_id=#{dishId}")
    void delectById(Long dishId);

    List<DishFlavor> selectByDishId(Long dishId);
}
