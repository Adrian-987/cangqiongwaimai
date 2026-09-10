package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import com.sky.exception.BaseException;
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
                //不能吞掉异常：填充失败会让 create_time/create_user 为空，
                //随后 SQL 报错难以定位。这里记录日志并直接抛出，让事务整体回滚。
                log.error("公共字段自动填充失败（INSERT），实体类型：{}", emp == null ? "null" : emp.getClass().getName(), e);
                throw new BaseException(MessageConstant.AUTO_FILL_FAILED);
            }
        } else if (operationType==OperationType.UPDATE) {
                try{
                    Method UpdateTime = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                    Method UpdateUser = emp.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                    UpdateTime.invoke(emp, localDateTime);
                    UpdateUser.invoke(emp, Id);
                } catch (Exception e) {
                    log.error("公共字段自动填充失败（UPDATE），实体类型：{}", emp == null ? "null" : emp.getClass().getName(), e);
                    throw new BaseException(MessageConstant.AUTO_FILL_FAILED);
                }
        }
    }

}
