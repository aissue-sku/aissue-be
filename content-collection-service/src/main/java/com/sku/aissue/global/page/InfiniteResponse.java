/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.page;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "무한스크롤 응답")
public class InfiniteResponse<T> {

  @Schema(description = "데이터 목록")
  private List<T> content;

  @Schema(description = "다음 페이지 요청 시 사용할 커서 (마지막 항목의 ID, 다음 데이터 없으면 null)")
  private Long lastCursor;

  @Schema(description = "다음 데이터 존재 여부")
  private Boolean hasNext;

  @Schema(description = "현재 반환된 데이터 개수")
  private Integer size;
}
