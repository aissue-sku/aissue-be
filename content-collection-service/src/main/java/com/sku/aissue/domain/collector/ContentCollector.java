/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.collector;

import java.util.List;

import com.sku.aissue.domain.dto.CollectedContentDto;
import com.sku.aissue.domain.entity.ContentSource;

public interface ContentCollector {

  List<CollectedContentDto> collect();

  ContentSource getSource();
}
