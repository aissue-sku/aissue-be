/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  public ResponseEntity<BaseResponse<List<IssueCardResponse>>> getIssueCards(
      @AuthenticationPrincipal String username) {
    List<IssueCardResponse> cards =
        username != null
            ? issueCardService.getPersonalizedCards(username)
            : issueCardService.getIssueCards();
    return ResponseEntity.ok(BaseResponse.success(cards));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> refreshIssueCards() {
    issueCardService.generateAndSaveByHotTopics();
    return ResponseEntity.ok(BaseResponse.success(null));
  }
}
