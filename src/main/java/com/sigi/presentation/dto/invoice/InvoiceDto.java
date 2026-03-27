package com.sigi.presentation.dto.invoice;

import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.order.OrderDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

import com.sigi.persistence.enums.InvoiceStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDto {
    private UUID id;
    private String number;
    private OrderDto order;
    private ClientDto client;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private InvoiceStatus status;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}