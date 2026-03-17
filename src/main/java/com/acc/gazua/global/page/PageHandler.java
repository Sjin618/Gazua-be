package com.acc.gazua.global.page;

import java.util.List;

public interface PageHandler <T> {

    boolean supports(String sortBy);

    List<T> fetch(Object searchId,String cursor,int limit);

    String createCursor(T lastItem);
}
