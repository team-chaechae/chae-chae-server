package com.project.chaechaeserver.presentation.controller.dataset;

import com.project.chaechaeserver.application.global.constants.ResCode;
import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.dataset.ResDatasetPostPreSignedUrlDTO;
import com.project.chaechaeserver.application.service.dataset.DatasetService;
import com.project.chaechaeserver.presentation.request.dataset.ReqDatasetPostPreSignedUrlDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.ADMIN;
import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.EMPLOYEE;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @PostMapping
    @Secured({ADMIN, EMPLOYEE})
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
