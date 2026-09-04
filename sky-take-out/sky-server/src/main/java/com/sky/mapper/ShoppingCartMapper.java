package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    ShoppingCart selectShopping(ShoppingCart shoppingCart);

    void updateByid(Long id, Integer number);

    void insertShopping(ShoppingCart shoppingCart);

    List<ShoppingCart> selectList(Long userId);

    void delectByUserId(Long userId);

    void delectById(Long id);
}
