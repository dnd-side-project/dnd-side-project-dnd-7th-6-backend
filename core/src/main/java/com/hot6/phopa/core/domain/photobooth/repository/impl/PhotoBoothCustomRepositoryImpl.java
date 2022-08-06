package com.hot6.phopa.core.domain.photobooth.repository.impl;

import com.hot6.phopa.core.common.model.dto.PageableParam;
import com.hot6.phopa.core.common.model.type.Status;
import com.hot6.phopa.core.domain.photobooth.model.entity.PhotoBoothEntity;
import com.hot6.phopa.core.domain.photobooth.repository.PhotoBoothCustomRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

import static com.hot6.phopa.core.domain.photobooth.model.entity.QPhotoBoothEntity.photoBoothEntity;
import static com.hot6.phopa.core.domain.photobooth.model.entity.QPhotoBoothLikeEntity.photoBoothLikeEntity;
import static com.hot6.phopa.core.domain.review.model.entity.QReviewEntity.reviewEntity;
import static com.hot6.phopa.core.domain.review.model.entity.QReviewTagEntity.reviewTagEntity;
import static com.hot6.phopa.core.domain.tag.model.entity.QTagEntity.tagEntity;
import static com.hot6.phopa.core.domain.user.model.entity.QUserEntity.userEntity;

@Repository
public class PhotoBoothCustomRepositoryImpl extends QuerydslRepositorySupport implements PhotoBoothCustomRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public PhotoBoothCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(PhotoBoothEntity.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }


    public List<PhotoBoothEntity> findByPhotoBoothId(Long photoBoothId) {
        return from(photoBoothEntity)
                .where(photoBoothEntity.id.eq(photoBoothId))
                .fetch();
    }

    @Override
    public List<PhotoBoothEntity> findByPointSet(Set<Point> crawlingPointSet) {
        return from(photoBoothEntity)
                .where(photoBoothEntity.point.in(crawlingPointSet))
                .fetch();
    }

    @Override
    public List<PhotoBoothEntity> findAllByUserLike(Long userId) {
        return from(photoBoothEntity)
                .join(photoBoothEntity.photoBoothLikeSet, photoBoothLikeEntity).fetchJoin()
                .join(photoBoothLikeEntity.user, userEntity).fetchJoin()
                .where(userEntity.id.eq(userId))
                .fetch();
    }

    @Override
    public PhotoBoothEntity findByIdWithTag(Long photoBoothId) {
        return from(photoBoothEntity)
                .leftJoin(photoBoothEntity.tag, tagEntity).fetchJoin()
                .leftJoin(photoBoothEntity.reviewSet, reviewEntity).fetchJoin()
                .leftJoin(reviewEntity.reviewTagSet, reviewTagEntity).fetchJoin()
                .where(photoBoothEntity.id.eq(photoBoothId))
                .fetchOne();
    }

    @Override
    public Page<PhotoBoothEntity> findByPhotoBoothIdAndColumn(List<Long> photoBoothIdList, Status status, Set<Long> tagIdSet, PageableParam pageable) {
        QueryResults result = jpaQueryFactory
                .selectFrom(photoBoothEntity)
                .leftJoin(photoBoothEntity.reviewSet, reviewEntity).fetchJoin()
                .leftJoin(reviewEntity.reviewTagSet, reviewTagEntity).fetchJoin()
                .leftJoin(reviewTagEntity.tag, tagEntity).fetchJoin()
                .where(photoBoothEntity.id.in(photoBoothIdList))
                .where(buildPredicate(status, tagIdSet))
                .orderBy(orderByFieldList(photoBoothIdList))
                .offset(pageable.getPage())
                .limit(pageable.getPageSize())
                .fetchResults();
        return new PageImpl<>(result.getResults(), PageRequest.of(pageable.getPage(), pageable.getPageSize()), result.getTotal());
    }

    private Predicate buildPredicate(Status status, Set<Long> tagIdSet) {
        BooleanBuilder builder = new BooleanBuilder();
        if (status != null) {
            builder.and(photoBoothEntity.status.eq(status));
        }
        if (CollectionUtils.isNotEmpty(tagIdSet)) {
            builder.and(photoBoothEntity.tag.id.in(tagIdSet).or(tagEntity.id.in(tagIdSet)));
        }
        return builder.getValue();
    }

    private OrderSpecifier<?> orderByFieldList(List<Long> photoBoothIdList) {
        return Expressions.stringTemplate("FIELD({0}, {1})", photoBoothEntity.id, photoBoothIdList)
                .asc();
    }
}
