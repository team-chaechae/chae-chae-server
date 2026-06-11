package com.project.deliveryservice.application.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResDTO<T> {

    private int code;
    private String message;
    private T data;

    private ResDTO(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResDTO<T> success(T data) {
        return new ResDTO<>(200, "success", data);
    }

    public static <T> ResDTO<T> created(T data) {
        return new ResDTO<>(201, "created", data);
    }
}
