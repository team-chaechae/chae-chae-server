package com.project.chaechaeserver.application.service.inventory;

import com.project.chaechaeserver.application.response.inventory.ResGetInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResInventorySearchDTO;
import com.project.chaechaeserver.application.response.inventory.ResSingleUpdateInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkSaleInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkUpdateInventoryDTO;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.presentation.request.inventory.ReqSingleUpdateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkUpdateInventoryDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    ResVulkCreateInventoryPostDTO createInventoryForVulk(ReqVulkCreateInventoryDTO dto);

    ResSingleUpdateInventoryDTO modifyInventoryForSingle(ReqSingleUpdateInventoryDTO dto);
    ResVulkSaleInventoryDTO decreaseInventoryForBulk(ReqVulkUpdateInventoryDTO dto);
    ResVulkUpdateInventoryDTO modifyInventoryForVulk(ReqVulkUpdateInventoryDTO dto);

    ResGetInventoryDTO getInventoryInfo(Long id);

    ResInventorySearchDTO getInventorySearchInfo(Pageable pageable,
        String productName,
        Boolean deletedAt,
        ProductStatusType productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,
        List<String> sortList);
}
