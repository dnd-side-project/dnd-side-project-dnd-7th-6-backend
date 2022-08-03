package com.hot6.phopa.core.domain.photobooth.repository;

import com.hot6.phopa.core.common.model.dto.PageableParam;
import com.hot6.phopa.core.common.model.type.Status;
import com.hot6.phopa.core.domain.photobooth.model.entity.PhotoBoothEntity;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

public interface PhotoBoothCustomRepository {
    List<PhotoBoothEntity> findByPointSet(Set<Point> crawlingPointSet);

    List<PhotoBoothEntity> findAllByUserLike(Long userId);

    PhotoBoothEntity findByIdWithTag(Long photoBoothId);

    Page<PhotoBoothEntity> findByPhotoBoothIdAndColumn(List<Long> photoBoothIds, Status status, Set<Long> tagIdSet, PageableParam pageable);
}
