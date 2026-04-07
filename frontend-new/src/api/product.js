import http from './http';

export function getProductListApi(params = {}) {
  return http.get('/product/list', { params });
}

export function getProductDetailApi(id) {
  return http.get(`/product/detail/${id}`);
}

export function getSellerProductListApi(params = {}) {
  return http.get('/product/seller/list', { params });
}

export function createSellerProductApi(payload) {
  return http.post('/product/seller', payload);
}

export function updateSellerProductApi(id, payload) {
  return http.put(`/product/seller/${id}`, payload);
}

export function deleteSellerProductApi(id) {
  return http.delete(`/product/seller/${id}`);
}

export function changeSellerProductStatusApi(id, status) {
  return http.post(`/product/seller/${id}/status`, { status });
}

export function adjustSellerProductStockApi(id, delta) {
  return http.post(`/product/seller/${id}/stock/adjust`, { delta });
}
