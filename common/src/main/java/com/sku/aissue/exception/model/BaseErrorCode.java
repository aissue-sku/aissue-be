/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.exception.model;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

  String getCode();

  String getMessage();

  HttpStatus getStatus();
}
