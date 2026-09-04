package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLDataException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHander(SQLIntegrityConstraintViolationException exception){
        String message=exception.getMessage();
        if(message.contains("Duplicate entry")){
            //加长度保护，防止消息格式不符合预期时数组越界
            String mess[]=message.split(" ");
            if (mess.length > 2) {
                String msg=mess[2]+ MessageConstant.ALREADY_EXITS;
                return Result.error(msg);
            }
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }else {
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

    /**
     * 兜底异常处理，保证所有异常都以统一Result格式返回，避免前端收到500白页
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Result unexpectedExceptionHandler(Exception ex){
        log.error("未知异常：", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
