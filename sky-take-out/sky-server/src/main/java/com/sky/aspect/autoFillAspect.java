package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import org.aspectj.lang.reflect.MethodSignature;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;
import java.time.LocalDateTime;

//通过aop实现公共字段填充,运用反射
@Slf4j
@Aspect
@Component
public class autoFillAspect {

    //获得切入点
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointcnt(){}

    @Before("autoFillPointcnt()")
    public void autoFill(JoinPoint joinPoint)  {
        log.info("进行公共字段填充");
        //反射获得方法该注解的属性
        MethodSignature signature=(MethodSignature) joinPoint.getSignature();
        AutoFill autoFill=signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType=autoFill.value();
        //得到方法的传入参数
        Object[] args=joinPoint.getArgs();
        Object emp=args[0];
        LocalDateTime localDateTime=LocalDateTime.now();
        Long Id= BaseContext.getCurrentId();
        //反射获得方法并且写入
        if(operationType==OperationType.INSERT){
            try {
                Method CreateTime = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method CreateUser = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method UpdateTime = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method UpdateUser = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                CreateTime.invoke(emp, localDateTime);
                CreateUser.invoke(emp, Id);
                UpdateTime.invoke(emp, localDateTime);
                UpdateUser.invoke(emp, Id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (operationType==OperationType.UPDATE) {
                try{
                    Method UpdateTime = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                    Method UpdateUser = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                    UpdateTime.invoke(emp, localDateTime);
                    UpdateUser.invoke(emp, Id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

}
