/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.page.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sku.aissue.page.response.InfiniteResponse;

@Component
public class InfiniteMapper {

  public <T> InfiniteResponse<T> toInfiniteResponse(
      List<T> content, Long lastCursor, boolean hasNext, int size) {
    return InfiniteResponse.<T>builder()
        .content(content)
        .lastCursor(lastCursor)
        .hasNext(hasNext)
        .size(size)
        .build();
  }
}
