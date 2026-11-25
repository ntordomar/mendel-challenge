package com.mendel.transactions.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull BigDecimal amount,
        @NotBlank String type,
        @JsonProperty("parent_id") Long parentId
) {
}
