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
