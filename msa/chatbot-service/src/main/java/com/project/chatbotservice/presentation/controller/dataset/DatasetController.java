package com.project.chatbotservice.presentation.controller.dataset;

import com.project.chatbotservice.application.global.constants.ResCode;
import com.project.chatbotservice.application.global.dto.ResDTO;
import com.project.chatbotservice.application.response.dataset.ResDatasetPostPreSignedUrlDTO;
import com.project.chatbotservice.application.service.dataset.DatasetService;
import com.project.chatbotservice.presentation.controller.dataset.docs.DatasetControllerSwagger;
import com.project.chatbotservice.presentation.request.dataset.ReqDatasetPostPreSignedUrlDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class DatasetController implements DatasetControllerSwagger {

    private final DatasetService datasetService;

    @PostMapping
    public ResponseEntity<ResDTO<ResDatasetPostPreSignedUrlDTO>> generatePreSignedUrl(@Valid @RequestBody ReqDatasetPostPreSignedUrlDTO dto) {

        return new ResponseEntity<>(
                ResDTO.<ResDatasetPostPreSignedUrlDTO>builder()
                        .code(ResCode.CREATED)
                        .message("Pre-Signed-Url 발급 완료")
                        .data(datasetService.generatePreSignedUrl(dto))
                        .build(),
                HttpStatus.CREATED
        );
    }
}
