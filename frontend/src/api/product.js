import http from './http';

export function getProductListApi(params = {}) {
  return http.get('/product/list', { params });
}
