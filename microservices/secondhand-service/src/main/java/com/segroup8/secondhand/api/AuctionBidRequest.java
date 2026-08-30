package com.segroup8.secondhand.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AuctionBidRequest(@NotNull @DecimalMin("0.01") BigDecimal bidAmount) {
}
