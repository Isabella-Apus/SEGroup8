import http from "./http";

export function listCouponCenterApi(params = {}) {
  return http.get("/voucher/list", { params });
}

export function listMyCouponsApi(params = {}) {
  return http.get("/voucher/my", { params });
}

export function claimCouponApi(voucherId) {
  return http.post(`/voucher/${voucherId}/claim`);
}

export function listCheckoutCouponsApi(params = {}) {
  return http.get("/voucher/my/available", { params });
}

export function listCheckoutCouponReasonsApi(params = {}) {
  return http.get("/voucher/my/available/reasons", { params });
}

export function listAdminCouponsApi(params = {}) {
  return http.get("/voucher/admin/list", { params });
}

export function createAdminCouponApi(payload) {
  return http.post("/voucher/admin", payload);
}

export function updateAdminCouponApi(voucherId, payload) {
  return http.put(`/voucher/admin/${voucherId}`, payload);
}

export function closeAdminCouponApi(voucherId) {
  return http.post(`/voucher/admin/${voucherId}/close`);
}

export function deleteAdminCouponApi(voucherId) {
  return http.delete(`/voucher/admin/${voucherId}`);
}
