import http from "./http";

export function getSecondhandListApi(params = {}) {
    return http.get("/secondhand/list", { params });
}

export function publishSecondhandApi(payload) {
    return http.post("/secondhand/publish", payload);
}
