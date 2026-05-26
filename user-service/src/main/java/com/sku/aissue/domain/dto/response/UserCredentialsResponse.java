/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCredentialsResponse {

  private String username;
  private String password;
  private String role;
}
