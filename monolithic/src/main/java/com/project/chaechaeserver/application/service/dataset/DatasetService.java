package com.project.chaechaeserver.application.service.dataset;

import com.project.chaechaeserver.application.response.dataset.ResDatasetPostPreSignedUrlDTO;
import com.project.chaechaeserver.presentation.request.dataset.ReqDatasetPostPreSignedUrlDTO;

public interface DatasetService {

    ResDatasetPostPreSignedUrlDTO generatePreSignedUrl(ReqDatasetPostPreSignedUrlDTO dto);

}
