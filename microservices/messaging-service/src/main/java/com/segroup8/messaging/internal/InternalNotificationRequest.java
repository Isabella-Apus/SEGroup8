package com.segroup8.messaging.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InternalNotificationRequest(
        @NotNull Long recipientUserId,
        @NotBlank String title,
        @NotBlank String content,
        String notificationType,
        String businessType,
        String businessId,
        String targetPath,
        String scope,
        @NotBlank String dedupeKey,
        @NotBlank String traceId) { }
