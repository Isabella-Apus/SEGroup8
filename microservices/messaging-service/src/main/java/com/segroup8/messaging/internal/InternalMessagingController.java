package com.segroup8.messaging.internal;

import com.segroup8.messaging.common.ApiResult;
import com.segroup8.messaging.delivery.DeliveryOutboxService;
import com.segroup8.messaging.event.EventEnvelope;
import com.segroup8.messaging.event.InboxEventService;
import com.segroup8.messaging.security.InternalServiceInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalMessagingController {
    private final InboxEventService inbox;
    private final InternalNotificationService notifications;
    private final DeliveryOutboxService delivery;
    public InternalMessagingController(InboxEventService inbox, InternalNotificationService notifications,
            DeliveryOutboxService delivery) {
        this.inbox = inbox; this.notifications = notifications; this.delivery = delivery;
    }

    @PostMapping("/events")
    public ApiResult<Map<String, Object>> ingress(@Valid @RequestBody EventEnvelope event) {
        boolean accepted = inbox.accept(event);
        return ApiResult.success(Map.of("eventId", event.eventId(), "accepted", accepted,
                "status", accepted ? "RECEIVED" : "DUPLICATE"));
    }

    @PostMapping("/notifications")
    public ApiResult<?> create(@Valid @RequestBody InternalNotificationRequest request, HttpServletRequest http) {
        return ApiResult.success(notifications.create(request, identity(http)));
    }

    @PostMapping("/events/replay/{eventId}")
    public ApiResult<Map<String, String>> replay(@PathVariable String eventId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            HttpServletRequest http) {
        String actualTrace = traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
        inbox.replay(eventId, identity(http), reason, actualTrace, delivery);
        return ApiResult.success(Map.of("eventId", eventId, "status", "RECEIVED", "traceId", actualTrace));
    }

    @GetMapping("/delivery/{dedupeKey}")
    public ApiResult<?> delivery(@PathVariable String dedupeKey) {
        return ApiResult.success(notifications.delivery(dedupeKey));
    }

    private String identity(HttpServletRequest request) {
        Object value = request.getAttribute(InternalServiceInterceptor.IDENTITY_ATTRIBUTE);
        return value == null ? "authenticated-service" : String.valueOf(value);
    }
}
