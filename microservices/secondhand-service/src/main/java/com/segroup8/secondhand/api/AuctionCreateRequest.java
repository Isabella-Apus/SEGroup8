package com.segroup8.secondhand.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AuctionCreateRequest(@NotNull Long productId,
        @NotNull @DecimalMin("0.01") BigDecimal startPrice,
        @NotNull @DecimalMin("0.01") BigDecimal incrementAmount,
        @NotNull @Min(1) @Max(10080) Integer durationMinutes) {
}
