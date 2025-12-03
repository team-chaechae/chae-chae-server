package com.project.chatbotservice.application.service.dataset;

import com.project.chatbotservice.application.response.dataset.ResDatasetPostPreSignedUrlDTO;
import com.project.chatbotservice.presentation.request.dataset.ReqDatasetPostPreSignedUrlDTO;

public interface DatasetService {

    ResDatasetPostPreSignedUrlDTO generatePreSignedUrl(ReqDatasetPostPreSignedUrlDTO dto);

}
