package com.segroup8.platform.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Map<String, String> FIELD_ALIAS = new HashMap<>();

    static {
        FIELD_ALIAS.put("username", "用户名");
        FIELD_ALIAS.put("password", "密码");
        FIELD_ALIAS.put("nickname", "昵称");
        FIELD_ALIAS.put("phone", "手机号");
        FIELD_ALIAS.put("email", "邮箱");
        FIELD_ALIAS.put("name", "名称");
        FIELD_ALIAS.put("description", "描述");
        FIELD_ALIAS.put("price", "价格");
        FIELD_ALIAS.put("originPrice", "原价");
        FIELD_ALIAS.put("salePrice", "售价");
        FIELD_ALIAS.put("stock", "库存");
        FIELD_ALIAS.put("quantity", "数量");
        FIELD_ALIAS.put("cover", "封面图片");
        FIELD_ALIAS.put("images", "商品图片");
        FIELD_ALIAS.put("categoryId", "一级分类");
        FIELD_ALIAS.put("subCategoryId", "二级分类");
        FIELD_ALIAS.put("receiverName", "收货人");
        FIELD_ALIAS.put("receiverPhone", "收货手机号");
        FIELD_ALIAS.put("province", "省份");
        FIELD_ALIAS.put("city", "城市");
        FIELD_ALIAS.put("detailAddress", "详细地址");
        FIELD_ALIAS.put("status", "状态");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "提交内容未通过校验，请检查后重试";
        return Result.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "查询条件填写有误，请检查后重试";
        return Result.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("提交内容未通过校验，请检查后重试");
        return Result.fail(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleBody(HttpMessageNotReadableException ex) {
        return Result.fail(400, "请求内容格式不正确，请检查输入后重试");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return Result.fail(400, "缺少必填参数：" + fieldName(ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return Result.fail(400, fieldName(ex.getName()) + "格式不正确，请检查后重试");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return Result.fail(400, "提交内容与现有数据冲突，请检查是否重复或缺少关联数据");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException ex) {
        String path = ex.getResourcePath();
        if (path == null || path.isBlank()) {
            path = "unknown";
        }
        return Result.fail(404, "接口不存在: " + path);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(500, extractMessage(ex));
    }

    private String extractMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        if (current.getMessage() != null && !current.getMessage().isBlank()) {
            return current.getMessage();
        }
        if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        return "Server error, please check database configuration and initialization";
    }

    private String fieldName(String field) {
        if (field == null || field.isBlank()) {
            return "参数";
        }
        return FIELD_ALIAS.getOrDefault(field, field);
    }
}
