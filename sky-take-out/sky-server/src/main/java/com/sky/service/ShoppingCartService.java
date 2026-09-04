package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void insertShopping(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> selectList();

    void delectByUserId();

    void updateSub(ShoppingCartDTO shoppingCartDTO);
}
