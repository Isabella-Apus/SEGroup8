package com.segroup8.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Receives catalog inventory outcomes without reading the catalog schema. */
@RestController
@RequestMapping("/internal/events")
class InventoryEventController {
    private final InventoryEventService service;

    InventoryEventController(InventoryEventService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void receive(@Valid @RequestBody EventEnvelope event) {
        service.receive(event);
    }

    record EventEnvelope(@NotBlank String eventId, @NotBlank String eventType,
            @NotBlank String producer, JsonNode payload) {}
}

@org.springframework.stereotype.Service
class InventoryEventService {
    private static final Set<String> SUPPORTED = Set.of(
            "InventoryReservationExpired.v1", "InventoryReservationReleased.v1");
    private final JdbcTemplate db;
    private final ObjectMapper json;

    InventoryEventService(JdbcTemplate db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    @Transactional
    void receive(InventoryEventController.EventEnvelope event) {
        if (!"catalog-shop-service".equals(event.producer()) || !SUPPORTED.contains(event.eventType())) {
            throw new OrderException("UNSUPPORTED_EVENT", "Unsupported inventory event", 400);
        }
        if (event.payload() == null || event.payload().path("orderId").asText().isBlank()) {
            throw new OrderException("INVALID_EVENT_PAYLOAD", "Inventory event orderId is required", 400);
        }
        try {
            db.update("insert into inbox_event(event_id,event_type,producer,payload) values(?,?,?,?)",
                    event.eventId(), event.eventType(), event.producer(), json.writeValueAsString(event.payload()));
        } catch (DuplicateKeyException duplicate) {
            return;
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }

        // Catalog's payload orderId is the external reservation key stored by order_info.
        // A late release after an explicit cancellation is therefore an idempotent no-op,
        // while an expiry closes only an order that is still awaiting payment.
        db.update("update order_info set order_status='CANCELLED',closed_time=current_timestamp,"
                        + "version=version+1 where reservation_id=? and order_status='PENDING_PAY'",
                event.payload().path("orderId").asText());
    }
}
