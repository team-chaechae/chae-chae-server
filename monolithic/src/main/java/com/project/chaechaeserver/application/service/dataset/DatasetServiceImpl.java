package com.project.chaechaeserver.application.service.dataset;

import com.project.chaechaeserver.application.response.dataset.ResDatasetPostPreSignedUrlDTO;
import com.project.chaechaeserver.presentation.request.dataset.ReqDatasetPostPreSignedUrlDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    @Value("${chatbot.base-url.fast-api}")
    private String baseUrl;

    public final RestClient restClient;

    public ResDatasetPostPreSignedUrlDTO generatePreSignedUrl(ReqDatasetPostPreSignedUrlDTO dto) {

        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/v1/datasets/upload")
                .encode()
                .build()
                .toUri();

        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(ResDatasetPostPreSignedUrlDTO.class);
    }
}
