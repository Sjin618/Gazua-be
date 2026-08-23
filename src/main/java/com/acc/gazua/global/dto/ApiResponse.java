package com.acc.gazua.global.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse <T>{
    private T data;
    private String message;

    //응답에 보낼 데이터가 없는 경우
    public static  ApiResponse<Void> success(String message){
        return new ApiResponse<>(null,message);
    }

    //실패 응답을 보내야 할 경우
    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(null, message);
    }

    //data와 message 둘 다 설정해야 하는 경우
    public static <T> ApiResponse<T> success(T data,String message){
        return new ApiResponse<>(data,message);
    }

    //data만 설정해서 보내야할 경우
    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(data,"success");
    }


}
