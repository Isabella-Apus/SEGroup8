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
