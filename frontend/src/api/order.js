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

export function getSellerOrderDetailApi(orderId) {
    return http.get(`/order/seller/detail/${orderId}`);
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

export function submitOrderReviewApi(orderId, payload) {
    return http.post(`/order/${orderId}/review`, payload);
}

export function submitOrderItemReviewsApi(orderId, payload) {
    return http.post(`/order/${orderId}/review/items`, payload);
}

export function refundOrderApi(orderId, payload) {
    return http.post(`/order/${orderId}/refund`, payload);
}

export function approveRefundOrderApi(orderId) {
    return http.post(`/order/${orderId}/refund/approve`);
}

export function rejectRefundOrderApi(orderId) {
    return http.post(`/order/${orderId}/refund/reject`);
}

export function remindShipOrderApi(orderId) {
    return http.post(`/order/${orderId}/remind-ship`);
}

export function getSellerOrderListApi(params = {}) {
    return http.get('/order/seller/list', { params });
}

export function shipOrderApi(orderId) {
    return http.post(`/order/${orderId}/ship`);
}
