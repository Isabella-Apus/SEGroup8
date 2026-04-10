import http from './http';

export function createOrderApi(payload) {
    return http.post('/order/create', payload);
}

export function getOrderListApi(params = {}) {
    return http.get('/order/list', { params });
}

export function getOrderDetailApi(orderId) {
    return http.get(`/order/detail/${orderId}`);
}

export function payOrderApi(orderId) {
    return http.post(`/order/${orderId}/pay`);
}

export function cancelOrderApi(orderId) {
    return http.post(`/order/${orderId}/cancel`);
}

export function confirmReceiveOrderApi(orderId) {
    return http.post(`/order/${orderId}/confirm-receive`);
}

export function completeOrderApi(orderId) {
    return http.post(`/order/${orderId}/complete`);
}

export function reviewOrderApi(orderId, payload) {
    return http.post(`/order/${orderId}/review`, payload);
}

export function reviewOrderItemsApi(orderId, payload) {
    return http.post(`/order/${orderId}/review/items`, payload);
}

export function refundOrderApi(orderId, payload = {}) {
    return http.post(`/order/${orderId}/refund`, payload);
}

export function remindShipApi(orderId) {
    return http.post(`/order/${orderId}/remind-ship`);
}

export function getSellerOrderListApi(params = {}) {
    return http.get('/order/seller/list', { params });
}

export function getSellerOrderDetailApi(orderId) {
    return http.get(`/order/seller/detail/${orderId}`);
}

export function shipSellerOrderApi(orderId) {
    return http.post(`/order/${orderId}/ship`);
}

export function approveSellerRefundApi(orderId) {
    return http.post(`/order/${orderId}/refund/approve`);
}

export function rejectSellerRefundApi(orderId) {
    return http.post(`/order/${orderId}/refund/reject`);
}
