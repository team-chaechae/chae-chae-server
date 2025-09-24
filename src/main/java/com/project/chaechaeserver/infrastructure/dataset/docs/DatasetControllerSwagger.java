package com.project.chaechaeserver.infrastructure.dataset.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.dataset.ResDatasetPostPreSignedUrlDTO;
import com.project.chaechaeserver.presentation.request.dataset.ReqDatasetPostPreSignedUrlDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Dataset", description = "파일 업로드 관련 API를 제공합니다.")
@RequestMapping("/api/dataset")
public interface DatasetControllerSwagger {


    @Operation(summary = "Pre-Signed-Url 생성", description = "Pre-Signed-Url 을 생성하는 API 입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "URL 생성 성공", content = @Content(schema = @Schema(implementation = ResDatasetPostPreSignedUrlDTO.class))),
            @ApiResponse(responseCode = "400", description = "URL 생성 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping
    ResponseEntity<ResDTO<ResDatasetPostPreSignedUrlDTO>> generatePreSignedUrl(@Valid @RequestBody ReqDatasetPostPreSignedUrlDTO dto);
}
