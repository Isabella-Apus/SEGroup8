import http from "./http";

export function getSecondhandListApi(params = {}) {
    return http.get("/secondhand/list", { params });
}

export function getMySecondhandListApi(params = {}) {
    return http.get("/secondhand/seller/list", { params });
}

export function createSecondhandApi(payload) {
    return http.post("/secondhand/seller", payload);
}

export function updateSecondhandApi(id, payload) {
    return http.put(`/secondhand/seller/${id}`, payload);
}

export function changeSecondhandStatusApi(id, status) {
    return http.post(`/secondhand/seller/${id}/status`, { status });
}

export function deleteSecondhandApi(id) {
    return http.delete(`/secondhand/seller/${id}`);
}

export function buySecondhandApi(id, payload = {}) {
    return http.post(`/secondhand/${id}/buy`, payload);
}

