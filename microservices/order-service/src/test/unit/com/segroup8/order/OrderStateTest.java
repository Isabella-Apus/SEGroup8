package com.segroup8.order;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("DOMAIN_C")
class OrderStateTest {
    @Test void validTransitionsAreAccepted() {
        assertThatCode(() -> OrderState.require(OrderState.PENDING_PAY, OrderState.Action.PAY)).doesNotThrowAnyException();
        assertThatCode(() -> OrderState.require(OrderState.PENDING_SHIP, OrderState.Action.SHIP)).doesNotThrowAnyException();
        assertThatCode(() -> OrderState.require(OrderState.SHIPPED, OrderState.Action.CONFIRM_RECEIVE)).doesNotThrowAnyException();
    }

    @Test void invalidTransitionIsRejectedWithStableCode() {
        assertThatThrownBy(() -> OrderState.require(OrderState.COMPLETED, OrderState.Action.PAY))
                .isInstanceOf(OrderException.class)
                .extracting(e -> ((OrderException)e).code()).isEqualTo("INVALID_ORDER_TRANSITION");
    }
}
