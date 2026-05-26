/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.response.IssueCardResponse;
import com.sku.aissue.domain.service.IssueCardService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class IssueCardControllerImpl implements IssueCardController {

  private final IssueCardService issueCardService;

  @Override
  public ResponseEntity<BaseResponse<List<IssueCardResponse>>> getIssueCards() {
    return ResponseEntity.ok(BaseResponse.success(issueCardService.getIssueCards()));
  }
}
