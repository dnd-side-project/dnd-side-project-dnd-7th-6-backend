package com.hot6.phopa.api.domain.photobooth.model.dto;

import com.hot6.phopa.core.domain.photobooth.model.dto.PhotoBoothDTO;
import com.hot6.phopa.core.domain.tag.model.dto.TagDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class PhotoBoothApiDTO {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class PhotoBoothApiResponse extends PhotoBoothDTO {
    }
    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    public static class PhotoBoothFormResponse {
        List<TagDTO> photoBoothTagList;
    }
}
