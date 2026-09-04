package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealFishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetMealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetMealServiceImpl implements SetMealService {
    @Autowired
    SetmealMapper setmealMapper;

    @Autowired
    SetmealFishMapper setmealFishMapper;

    @Autowired
    DishMapper dishMapper;

    @Override
    @Transactional
    public void insertSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insertSetmeal(setmeal);
        List<SetmealDish> list=setmealDTO.getSetmealDishes();
        //判空防止NPE
        if (list != null && list.size() > 0) {
            list.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmeal.getId());
            });
            setmealFishMapper.insert(list);
        }
    }

    @Override
    public PageResult selectPage(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page=setmealMapper.selectPage(setmealPageQueryDTO);
        Long total= page.getTotal();
        return new PageResult(total,page.getResult());
    }

    @Transactional
    @Override
    public void delectByIds(List<Long> ids) {
        for (Long id:ids) {
            Setmeal setmeal = setmealMapper.selectById(id);
            if(StatusConstant.ENABLE==setmeal.getStatus()){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        ids.forEach(id->{
            setmealMapper.delectById(id);
            setmealFishMapper.delectBySetmealId(id);
        });
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        if(status==StatusConstant.ENABLE){
            List<Dish> list=dishMapper.selectByMealId(id);
            if (list!=null&&list.size()>0){
                list.forEach(dish -> {
                    if (dish.getStatus()!=StatusConstant.ENABLE){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        setmealMapper.updateStatus(status,id);
    }

    @Override
    public SetmealVO selectByid(Long id) {
        Setmeal setmeal=setmealMapper.selectById(id);
        List<SetmealDish> list=setmealFishMapper.selectBySetmealId(id);
        SetmealVO setmealVO=new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(list);
        return setmealVO;
    }

    @Transactional
    @Override
    public void updateMeal(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.updateMeal(setmeal);
        setmealFishMapper.delectBySetmealId(setmealDTO.getId());
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        //判空防止NPE
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDTO.getId());
            });
            setmealFishMapper.insert(setmealDishes);
        }
    }


    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }


    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
