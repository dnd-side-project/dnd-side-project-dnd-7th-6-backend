package com.hot6.phopa.core.domain.review.model.mapper;

import com.hot6.phopa.core.common.mapper.GenericMapper;
import com.hot6.phopa.core.domain.review.model.dto.TagDTO;
import com.hot6.phopa.core.domain.review.model.entity.TagEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class TagMapper extends GenericMapper<TagDTO, TagEntity> {
}
