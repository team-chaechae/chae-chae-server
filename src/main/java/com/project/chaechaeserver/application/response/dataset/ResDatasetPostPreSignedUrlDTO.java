package com.project.chaechaeserver.application.response.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResDatasetPostPreSignedUrlDTO {

    private String key;
    private String upload_url;

}
