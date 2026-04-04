import http from './http';

export function createOrderApi(payload) {
    return http.post('/order/create', payload);
}

export function getOrderListApi(params = {}) {
    return http.get('/order/list', { params });
}
