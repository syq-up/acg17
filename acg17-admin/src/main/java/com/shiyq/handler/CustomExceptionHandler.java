package com.shiyq.handler;

import com.shiyq.entity.VO.ResultVO;
import com.shiyq.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResultVO> handleApiException(ApiException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultVO> handleValidationException(MethodArgumentNotValidException exception) {
        return error(HttpStatus.BAD_REQUEST,
                firstErrorMessage(exception.getBindingResult().getAllErrors(), "请求参数不合法"));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ResultVO> handleMethodValidationException(
            HandlerMethodValidationException exception) {
        return error(HttpStatus.BAD_REQUEST,
                firstErrorMessage(exception.getAllErrors(), "请求参数不合法"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultVO> handleConstraintViolationException(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("请求参数不合法");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultVO> handleUnreadableMessageException(
            HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResultVO> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "请求参数类型错误: " + exception.getName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResultVO> handleMissingParameterException(
            MissingServletRequestParameterException exception) {
        return error(HttpStatus.BAD_REQUEST, "缺少请求参数: " + exception.getParameterName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ResultVO> handleMissingPartException(
            MissingServletRequestPartException exception) {
        return error(HttpStatus.BAD_REQUEST, "缺少上传内容: " + exception.getRequestPartName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultVO> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "上传文件或请求体过大");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ResultVO> handleMultipartException(MultipartException exception) {
        return error(HttpStatus.BAD_REQUEST, "上传请求格式错误");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResultVO> handleUnsupportedMediaTypeException(
            HttpMediaTypeNotSupportedException exception) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的请求内容类型");
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

    private String firstErrorMessage(List<? extends MessageSourceResolvable> errors,
                                     String fallback) {
        return errors.stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(message -> message != null && !message.trim().isEmpty())
                .orElse(fallback);
    }
}
