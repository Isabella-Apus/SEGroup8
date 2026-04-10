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
