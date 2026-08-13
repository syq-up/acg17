package com.shiyq.handler;

import com.shiyq.entity.VO.ResultVO;
import com.shiyq.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResultVO> handleApiException(ApiException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultVO> handleValidationException(MethodArgumentNotValidException exception) {
        ObjectError objectError = exception.getBindingResult().getAllErrors().get(0);
        return error(HttpStatus.BAD_REQUEST, objectError.getDefaultMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResultVO> handleIllegalArgumentException(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ResultVO> handleIOException(IOException exception) {
        log.error("处理请求时发生 IO 异常", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务器出现IO异常，请稍后再试吧。。。");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultVO> handleException(Exception exception) {
        log.error("处理请求时发生未预期异常", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后再试");
    }

    private ResponseEntity<ResultVO> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ResultVO.error(status.value(), message));
    }
}
