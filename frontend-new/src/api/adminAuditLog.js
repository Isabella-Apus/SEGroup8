import http from "./http";

export function pageAdminAuditLogsApi(params) {
    return http.get("/admin/audit-logs", { params });
}
