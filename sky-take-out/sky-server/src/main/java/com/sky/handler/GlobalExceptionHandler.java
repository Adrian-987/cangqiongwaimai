package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 * 响应分层约定：
 *  - 可预期的业务错误（如密码错误、起售中不能删除）：HTTP 200 + Result(code=0,msg)，前端按 body 里的 code 弹提示
 *  - 未预期的系统故障：HTTP 500 + Result(code=0,msg)，让状态码本身成为错误信号，网络面板一眼可见
 *  - 客户端请求本身不合法（JSON解析失败等）：HTTP 400 + Result(code=0,msg)
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
     * 请求体解析失败（JSON格式错误、字段类型不匹配）：属于客户端问题，返回400并给出明确提示
     * @param ex
     * @return
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result httpMessageNotReadableHandler(HttpMessageNotReadableException ex){
        log.error("请求体解析失败：{}", ex.getMessage());
        return Result.error(MessageConstant.REQUEST_BODY_INVALID);
    }

    /**
     * 兜底异常处理：未预期的异常返回500状态码，让前端/网络面板能直接看出请求失败，
     * body 仍保持统一 Result 格式（微信小程序对任何状态码都走success回调，不影响读取msg）
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result unexpectedExceptionHandler(Exception ex){
        log.error("未知异常：", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
