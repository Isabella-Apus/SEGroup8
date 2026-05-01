import http from "./http";

export function pageAdminVoucherApi(params = {}) {
  return http.get('/voucher/admin/list', { params });
}

export function createAdminVoucherApi(data) {
  return http.post('/voucher/admin', data);
}

export function updateAdminVoucherApi(id, data) {
  return http.put(`/voucher/admin/${id}`, data);
}

export function closeAdminVoucherApi(id) {
  return http.post(`/voucher/admin/${id}/close`);
}

export function deleteAdminVoucherApi(id) {
  return http.delete(`/voucher/admin/${id}`);
}
