package com.sigi.presentation.dto.user;

import com.sigi.presentation.dto.product.ProductDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "UserResponse", description = "Response containing details of a user record")
public class MetricsDto {
    private Integer totalProducts;
    private Integer totalInventories;
    private Integer totalWarehouses;
    private Integer totalLoWStock;
    private Integer totalOrdersDraft;
    private Integer totalOrdersConfirmed;
    private Integer totalOrdersCanceled;
    private Integer totalOrdersDelivered;
    private Integer totalOrdersPending;
    private List<ProductDto> latestProductsAdded;
}
