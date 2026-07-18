package com.acc.gazua.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

//null 인 필드는 JSON으로 변환할 때 제외됨
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomCursor(
    String cursorValue,
    Long cursorId)
{

}
