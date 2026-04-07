import http from "./http";

export function submitMerchantApplicationApi(payload) {
    return http.post("/user/merchant-application", payload);
}

export async function getMyMerchantApplicationApi() {
    try {
        return await http.get("/user/merchant-application/me");
    } catch (error) {
        return http.get("/user/merchant-application/me.");
    }
}

export function pageMerchantApplicationsApi(params) {
    return http.get("/admin/merchant-applications", { params });
}

export function approveMerchantApplicationApi(id) {
    return http.post(`/admin/merchant-applications/${id}/approve`);
}

export function rejectMerchantApplicationApi(id, payload) {
    return http.post(`/admin/merchant-applications/${id}/reject`, payload);
}
