package com.project.chaechaeserver.presentation.request.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqDatasetPostPreSignedUrlDTO {

    @Schema(example = "채채봇 매뉴얼.pdf")
    @NotBlank(message = "파일명을 입력해주세요.")
    private String filename;

}
