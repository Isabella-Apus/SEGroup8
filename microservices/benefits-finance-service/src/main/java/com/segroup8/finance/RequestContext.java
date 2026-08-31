package com.segroup8.finance;

final class RequestContext {
    private static final ThreadLocal<Caller> CURRENT = new ThreadLocal<>();

    private RequestContext() {}

    static void set(Caller caller) { CURRENT.set(caller); }
    static void clear() { CURRENT.remove(); }

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

    record Caller(Long userId, String role, String requestId) {}
}
