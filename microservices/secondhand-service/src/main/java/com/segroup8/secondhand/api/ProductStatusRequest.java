package com.segroup8.secondhand.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull @Min(1) @Max(2) Integer status) {
}
