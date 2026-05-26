/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.exception;

import com.sku.aissue.exception.model.BaseErrorCode;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

  private final BaseErrorCode errorCode;

  public CustomException(BaseErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
