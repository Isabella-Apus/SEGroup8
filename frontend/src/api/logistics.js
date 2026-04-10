import http from "./http";

export function pushNextLogisticsApi(orderId) {
    return http.post("/logistics/push-next", { orderId });
}

export function getLogisticsTraceApi(orderId) {
    return http.get(`/logistics/order/${orderId}/trace`);
}
