package com.sigi.presentation.dto.invoice;

import lombok.*;

import java.util.UUID;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewInvoiceDto {
    private UUID orderId;
    private BigDecimal tax;
}
