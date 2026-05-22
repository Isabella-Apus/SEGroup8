import http from "./http";

export function getSecondhandListApi(params = {}) {
    return http.get("/secondhand/list", { params });
}

export function getSecondhandDetailApi(id) {
    return http.get(`/secondhand/detail/${id}`);
}

export function publishSecondhandApi(payload) {
    return http.post("/secondhand/seller", payload);
}

export function buySecondhandApi(id, payload = {}) {
    return http.post(`/secondhand/${id}/buy`, payload);
}

export function getSellerSecondhandListApi(params = {}) {
    return http.get('/secondhand/seller/list', { params });
}

export function updateSellerSecondhandApi(id, payload) {
    return http.put(`/secondhand/seller/${id}`, payload);
}

export function deleteSellerSecondhandApi(id) {
    return http.delete(`/secondhand/seller/${id}`);
}

export function changeSellerSecondhandStatusApi(id, status) {
    return http.post(`/secondhand/seller/${id}/status`, { status });
}

export function applyBargainApi(payload) {
    return http.post("/secondhand/trade/bargain/apply", payload);
}

export function confirmBargainApi(payload) {
    return http.post("/secondhand/trade/bargain/confirm", payload);
}

export function rejectBargainApi(negotiationId) {
    return http.post(`/secondhand/trade/bargain/${negotiationId}/reject`);
}

export function listBargainRequestsApi(params = {}) {
    return http.get("/secondhand/trade/bargain/list", { params });
}

export function getMyEffectiveBargainApi(productId) {
    return http.get("/secondhand/trade/bargain/effective", { params: { productId }, silent: true });
}

export function createAuctionApi(payload) {
    return http.post("/secondhand/trade/auction", payload);
}

export function getAuctionByProductIdApi(productId) {
    return http.get(`/secondhand/trade/auction/product/${productId}`, { silent: true });
}

export function getMyAuctionListApi(params = {}) {
    return http.get("/secondhand/trade/auction/seller/list", { params });
}

export function closeAuctionEarlyApi(auctionId) {
    return http.post(`/secondhand/trade/auction/${auctionId}/close`);
}

export function markAuctionFlowApi(auctionId) {
    return http.post(`/secondhand/trade/auction/${auctionId}/flow`);
}

export function placeAuctionBidApi(auctionId, payload) {
    return http.post(`/secondhand/trade/auction/${auctionId}/bid`, payload);
}
