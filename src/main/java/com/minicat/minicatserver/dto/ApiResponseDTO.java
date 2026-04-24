package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseDTO<T> {
    private Integer code;
    private String message;
    private T data;
    private Boolean success;  // 添加success字段供前端判断

    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>(200, "success", data, true);
    }

    public static <T> ApiResponseDTO<T> success() {
        return new ApiResponseDTO<>(200, "success", null, true);
    }

    public static <T> ApiResponseDTO<T> error(Integer code, String message) {
        return new ApiResponseDTO<>(code, message, null, false);
    }

    public static <T> ApiResponseDTO<T> error(String message) {
        return new ApiResponseDTO<>(500, message, null, false);
    }
}
