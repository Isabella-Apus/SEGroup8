import http from "./http";

export function createOrderApi(payload) {
    return http.post("/order/create", payload);
}

export function getOrderListApi(params = {}) {
    const query = { ...params };
    if (query.orderType && !query.productType) {
        query.productType = query.orderType;
    }
    return http.get("/order/list", { params: query });
}

export function getOrderDetailApi(orderId, config = {}) {
    return http.get(`/order/detail/${orderId}`, config);
}

export function getSellerOrderListApi(params = {}) {
    const query = { ...params };
    if (query.orderType && !query.productType) {
        query.productType = query.orderType;
    }
    return http.get("/order/seller/list", { params: query });
}

export function getSoldSecondhandOrderListApi(params = {}) {
    return getSellerOrderListApi({ ...params, productType: "SECONDHAND" });
}

export function getSellerOrderDetailApi(orderId, config = {}) {
    return http.get(`/order/seller/detail/${orderId}`, config);
}

export function payOrderApi(orderId, payload = {}) {
    return http.post(`/order/${orderId}/pay`, payload);
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

export function reviewOrderApi(orderId, payload) {
    return submitOrderReviewApi(orderId, payload);
}

export function submitOrderItemReviewsApi(orderId, payload) {
    return http.post(`/order/${orderId}/review/items`, payload);
}

export function reviewOrderItemsApi(orderId, payload) {
    return submitOrderItemReviewsApi(orderId, payload);
}

export function refundOrderApi(orderId, payload = {}) {
    return http.post(`/order/${orderId}/refund`, payload);
}

export function remindShipOrderApi(orderId) {
    return http.post(`/order/${orderId}/remind-ship`);
}

export function remindShipApi(orderId) {
    return remindShipOrderApi(orderId);
}

export function shipOrderApi(orderId, payload = {}) {
    return http.post(`/order/${orderId}/ship`, payload, { silent: true });
}

export function shipSellerOrderApi(orderId) {
    return shipOrderApi(orderId);
}

export function approveRefundOrderApi(orderId) {
    return http.post(`/order/${orderId}/refund/approve`);
}

export function approveSellerRefundApi(orderId) {
    return approveRefundOrderApi(orderId);
}

export function rejectRefundOrderApi(orderId) {
    return http.post(`/order/${orderId}/refund/reject`);
}

export function rejectSellerRefundApi(orderId) {
    return rejectRefundOrderApi(orderId);
}
