package com.acc.gazua.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CursorPage <T>{
    private List<T> content;
    private String nextCursorValue; // 정렬 기준 + "_" + cursorId
    private boolean hasNext;
}
