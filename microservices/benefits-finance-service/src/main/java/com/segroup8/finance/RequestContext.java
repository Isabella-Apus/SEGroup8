package com.segroup8.finance;

final class RequestContext {
    private static final ThreadLocal<Caller> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private RequestContext() {}

    static void set(Caller caller) { CURRENT.set(caller); }
    static void setTraceId(String traceId) { TRACE_ID.set(traceId); }
    static String traceId() { return TRACE_ID.get(); }
    static void clear() { CURRENT.remove(); TRACE_ID.remove(); }

    static Caller requireUser() {
        Caller caller = CURRENT.get();
        if (caller == null || caller.userId() == null) {
            throw new DomainException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "AUTH_REQUIRED", "缺少有效用户身份");
        }
        return caller;
    }

    static Caller requireRole(String role) {
        Caller caller = requireUser();
        if (!role.equals(caller.role())) {
            throw DomainException.forbidden("ROLE_FORBIDDEN", "当前身份无权执行该操作");
        }
        return caller;
    }

    static String requireUserOrRequestId() {
        Caller caller = CURRENT.get();
        return caller == null ? "internal-" + (traceId() == null ? "unknown" : traceId()) : caller.requestId();
    }

    record Caller(Long userId, String role, String requestId) {}
}
