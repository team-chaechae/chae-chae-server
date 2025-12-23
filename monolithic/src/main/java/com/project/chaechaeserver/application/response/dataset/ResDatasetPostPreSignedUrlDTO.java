package com.project.chaechaeserver.application.response.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResDatasetPostPreSignedUrlDTO {

    @Schema(example = "datasets/2025/09/24/채채봇/ce8abe89")
    private String key;

    @Schema(example = "http://localhost:9000/demo-bucket/datasets/2025/09/24/%EC%B1%84%EC%B1%84....")
    private String upload_url;

}
