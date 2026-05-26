/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddPointsRequest {

  private int points;
  private String reason;
}
