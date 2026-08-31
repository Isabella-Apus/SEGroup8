package com.segroup8.finance.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.segroup8.finance.ApiModels.VoucherSave;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VoucherRulesTest {
    private final jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void beanValidationRejectsZeroQuantityNegativeThresholdAndMissingDiscount() {
        VoucherSave invalid = new VoucherSave("券", "AMOUNT", null, null, new BigDecimal("-1"), 0,
                Instant.now(), Instant.now().plusSeconds(60), 1L, null, "SHOP");
        assertThat(validator.validate(invalid)).extracting(v -> v.getPropertyPath().toString())
                .contains("minAmount", "totalCount");
    }

    @Test
    void validRateVoucherPassesFieldValidation() {
        VoucherSave valid = new VoucherSave("八折券", "RATE", null, new BigDecimal("0.80"), BigDecimal.TEN, 5,
                Instant.now(), Instant.now().plusSeconds(600), 1L, null, "SHOP");
        assertThat(validator.validate(valid)).isEmpty();
    }
}
