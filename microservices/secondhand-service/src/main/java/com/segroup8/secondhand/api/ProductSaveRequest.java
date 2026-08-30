package com.segroup8.secondhand.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductSaveRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 255) String cover,
        @NotEmpty @Size(max = 9) List<@NotBlank @Size(max = 255) String> images,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal originPrice,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal salePrice,
        @NotNull Integer categoryId,
        @NotNull Integer subCategoryId,
        @NotBlank @Size(max = 30) String conditionLevel,
        @NotNull @Min(0) @Max(1) Integer isNegotiable,
        @Min(1) @Max(2) Integer status) {
}
