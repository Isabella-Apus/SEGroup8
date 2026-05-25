import http from "./http";

export function pageProductRiskAuditsApi(params = {}) {
  return http.get("/admin/product-risk-audits", { params });
}

export function decideProductRiskAuditApi(id, payload) {
  return http.post(`/admin/product-risk-audits/${id}/decision`, payload);
}
