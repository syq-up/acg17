package com.shiyq.handler;

import com.shiyq.constant.HttpStatus;
import com.shiyq.entity.VO.ResultVO;
import org.springframework.validation.ObjectError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultVO> MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        // 拿到异常
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        // 返回前端400异常，并给出错误信息
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultVO.error(HttpStatus.BAD_REQUEST, objectError.getDefaultMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResultVO IOExceptionHandler(IOException e) {
        System.out.println(e.getMessage());
        // 返回前端IO错误
        return ResultVO.error("服务器出现IO异常，请稍后再试吧。。。");
    }

}
