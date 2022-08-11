package com.hot6.phopa.api.domain.community.service;

import com.hot6.phopa.api.domain.community.model.dto.PostApiDTO;
import com.hot6.phopa.api.domain.community.model.dto.PostApiDTO.PostApiResponse;
import com.hot6.phopa.api.domain.community.model.dto.PostApiDTO.PostCreateRequest;
import com.hot6.phopa.api.domain.community.model.dto.PostApiDTO.PostFilterForm;
import com.hot6.phopa.api.domain.community.model.dto.PostApiDTO.PostForm;
import com.hot6.phopa.api.domain.community.model.mapper.PostApiMapper;
import com.hot6.phopa.core.common.exception.ApplicationErrorException;
import com.hot6.phopa.core.common.exception.ApplicationErrorType;
import com.hot6.phopa.core.common.model.dto.PageableParam;
import com.hot6.phopa.core.common.model.dto.PageableResponse;
import com.hot6.phopa.core.common.model.type.Status;
import com.hot6.phopa.core.common.service.S3UploadService;
import com.hot6.phopa.core.domain.community.model.entity.PostEntity;
import com.hot6.phopa.core.domain.community.model.entity.PostImageEntity;
import com.hot6.phopa.core.domain.community.model.entity.PostLikeEntity;
import com.hot6.phopa.core.domain.community.model.entity.PostTagEntity;
import com.hot6.phopa.core.domain.community.service.PostService;
import com.hot6.phopa.core.domain.tag.enumeration.TagType;
import com.hot6.phopa.core.domain.tag.model.dto.TagDTO;
import com.hot6.phopa.core.domain.tag.model.entity.TagEntity;
import com.hot6.phopa.core.domain.tag.model.mapper.TagMapper;
import com.hot6.phopa.core.domain.tag.service.TagService;
import com.hot6.phopa.core.domain.user.model.entity.UserEntity;
import com.hot6.phopa.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostApiService {

    private final PostService postService;

    private final PostApiMapper postApiMapper;

    private final S3UploadService s3UploadService;

    private final UserService userService;

    private final TagService tagService;

    private final TagMapper tagMapper;

    @Value("${cloud.aws.s3.upload.path.review}")
    private String reviewPath;

    @Transactional(readOnly = true)
    public List<PostApiResponse> getPosts() {
        List<PostEntity> postEntityList = postService.getAllPost();
        return postApiMapper.toDtoList(postEntityList);
    }

    public PostApiResponse createPost(PostCreateRequest postCreateRequest, List<MultipartFile> postImageList) {
        postCreateRequest.validCheck();
        fileInvalidCheck(postImageList);
        UserEntity userEntity = userService.findById(postCreateRequest.getUserId());
        PostEntity postEntity = PostEntity.builder()
                .title(postCreateRequest.getTitle())
                .content(postCreateRequest.getContent())
                .likeCount(0)
                .status(Status.ACTIVE)
                .user(userEntity)
                .build();
        if (CollectionUtils.isNotEmpty(postCreateRequest.getTagIdList())) {
            Set<PostTagEntity> postTagEntitySet = new HashSet<>();
            List<TagEntity> tagEntityList = tagService.getTagList(postCreateRequest.getTagIdList());
            for (TagEntity tagEntity : tagEntityList) {
                postTagEntitySet.add(
                        PostTagEntity.builder()
                                .post(postEntity)
                                .tag(tagEntity)
                                .build()
                );
                tagEntity.updatePostCount(1);
            }
            postEntity.setPostTagSet(postTagEntitySet);
        }

        if (CollectionUtils.isNotEmpty(postImageList)) {
            Set<PostImageEntity> postImageEntitySet = new HashSet<>();
            int index = 1;
            try {
                for (MultipartFile reviewImage : postImageList) {
                    String imageUrl = s3UploadService.uploadFiles(reviewImage, reviewPath);
                    postImageEntitySet.add(
                            PostImageEntity.builder()
                                    .post(postEntity)
                                    .imageUrl(imageUrl)
                                    .imageOrder(index++).
                                    build()
                    );
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            postEntity.setPostImageSet(postImageEntitySet);
        }

        return postApiMapper.toDto(postService.createPost(postEntity));
    }

    public void fileInvalidCheck(List<MultipartFile> imageList) {
        if (CollectionUtils.isNotEmpty(imageList)) {
            for (MultipartFile file : imageList) {
                if (Arrays.asList("jpg", "jpeg", "png").contains(FilenameUtils.getExtension(file.getOriginalFilename())) == false) {
                    throw new ApplicationErrorException(ApplicationErrorType.INVALID_REQUEST);
                }
            }
        }
    }

    public void like(Long postId, Long userId) {
        PostEntity postEntity = postService.getPostById(postId);
        UserEntity userEntity = userService.findById(userId);
        PostLikeEntity postLikeEntity = postService.getPostLikeByPostIdAndUserId(postId, userId);
        if (postLikeEntity != null){
            postService.deletePostLikeEntity(postLikeEntity);
            postEntity.updateLikeCount(-1);
        } else {
            postLikeEntity = PostLikeEntity.builder()
                    .post(postEntity)
                    .user(userEntity)
                    .build();
            postService.createPostLikeEntity(postLikeEntity);
            postEntity.updateLikeCount(1);
        }
    }

    public PostApiResponse getPost(Long postId) {
        return postApiMapper.toDto(postService.getPostById(postId));
    }


    public PageableResponse<PostApiResponse> getPostsByTagIdSet(Set<Long> tagIdSet, String order, PageableParam pageable) {
        log.info("order : {}", order);
        Page<PostEntity> postEntityPage = null;
        if(order==null) {
            postEntityPage = postService.getPostByTagIdSet(tagIdSet, pageable);
        } else if(order.equals("popular")) {
            postEntityPage = postService.getPostByTagIdSetOrderByLikeCountDesc(tagIdSet, pageable);
        } else if(order.equals("latest")) {
            postEntityPage = postService.getPostByTagIdSetOrderByCreatedAtDesc(tagIdSet, pageable);
        }
        
//        Page<PostEntity> postEntityPage = postService.getPostByTagIdSet(tagIdSet, pageable);
        List<PostApiResponse> postApiResponseList = postApiMapper.toDtoList(postEntityPage.getContent());
        return PageableResponse.makeResponse(postEntityPage, postApiResponseList);
    }

    public PostFilterForm getFilterFormData() {
        List<TagDTO> tagDTOList = tagMapper.toDtoList(tagService.getTagListByTagTypeList(TagType.POST_TAG_LIST, null));
        List<TagDTO> brandTagList = new ArrayList<>();
        Map<TagType, List<TagDTO>> personalTagList = new HashMap<>();
        Map<TagType, List<TagDTO>> conceptTagList = new HashMap<>();
        List<TagDTO> frameTagList = new ArrayList<>();
        Map<TagType, List<TagDTO>> tagTypeListMap = tagDTOList.stream().collect(Collectors.groupingBy(TagDTO::getTagType));
        for(Map.Entry<TagType, List<TagDTO>> entry : tagTypeListMap.entrySet()){
            if(TagType.BRAND.equals(entry.getKey())){
                brandTagList = entry.getValue();
            } else if (TagType.FRAME.equals(entry.getKey())){
                frameTagList = entry.getValue();
            } else if (TagType.PERSONAL_TAG_LIST.contains(entry.getKey())){
                personalTagList.put(entry.getKey(), entry.getValue());
            } else if (TagType.CONCEPT_TAG_LIST.contains(entry.getKey())){
                conceptTagList.put(entry.getKey(), entry.getValue());
            }
        }
        return PostFilterForm.of(brandTagList, personalTagList, conceptTagList, frameTagList);
    }

    public PostForm getFormData() {
        List<TagDTO> tagDTOList = tagMapper.toDtoList(tagService.getTagListByTagTypeList(TagType.POST_TAG_LIST, null));
        Map<TagType, List<TagDTO>> tagTypeListMap = tagDTOList.stream().collect(Collectors.groupingBy(TagDTO::getTagType));
        return PostForm.of(tagTypeListMap);
    }
}
