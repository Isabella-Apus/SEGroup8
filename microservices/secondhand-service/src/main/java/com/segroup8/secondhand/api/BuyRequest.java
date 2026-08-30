package com.segroup8.secondhand.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BuyRequest(@NotNull Long addressId, @Size(max = 255) String remark) {
}
