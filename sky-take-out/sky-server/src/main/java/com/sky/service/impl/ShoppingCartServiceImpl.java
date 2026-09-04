package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    ShoppingCartMapper shoppingCartMapper;
    @Autowired
    DishMapper dishMapper;
    @Autowired
    SetmealMapper setmealMapper;

    @Override
    public void insertShopping(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart=new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        ShoppingCart shoppingCart1=shoppingCartMapper.selectShopping(shoppingCart);
        if (shoppingCart1!=null){
            Integer number = shoppingCart1.getNumber()+1;
            Long id=shoppingCart1.getId();
            shoppingCartMapper.updateByid(id,number);
        }
        else {
            Long dishId=shoppingCart.getDishId();
            if (dishId!=null){
                Dish dish = dishMapper.selectById(dishId);
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
            }else {
                Long setMealId=shoppingCart.getSetmealId();
                Setmeal setmeal=setmealMapper.selectById(setMealId);
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insertShopping(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> selectList() {
        Long userId=BaseContext.getCurrentId();
        return shoppingCartMapper.selectList(userId);
    }

    @Override
    public void delectByUserId() {
        Long userId=BaseContext.getCurrentId();
        shoppingCartMapper.delectByUserId(userId);
    }

    @Override
    public void updateSub(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart=new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        ShoppingCart shoppingCart1=shoppingCartMapper.selectShopping(shoppingCart);
        if (shoppingCart1!=null){
            Integer number=shoppingCart1.getNumber();
            if(number==1){
                shoppingCartMapper.delectById(shoppingCart1.getId());
            }else {
                shoppingCartMapper.updateByid(shoppingCart1.getId(),number-1);
            }
        }else {
            throw new RuntimeException(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
