import http from "./http";

export async function getProfileApi() {
    try {
        return await http.get("/user/me");
    } catch (error) {
        return http.get("/user/profile");
    }
}

export function updateProfileApi(payload) {
    return http.put("/user/profile", payload);
}

export function listAddressesApi() {
    return http.get("/user/addresses");
}

export function createAddressApi(payload) {
    return http.post("/user/addresses", payload);
}

export function updateAddressApi(addressId, payload) {
    return http.put(`/user/addresses/${addressId}`, payload);
}

export function deleteAddressApi(addressId) {
    return http.delete(`/user/addresses/${addressId}`);
}

export function getBrowseHistoryApi() {
    return http.get("/user/browse-history");
}

export function deleteBrowseHistoryApi(historyId) {
    return http.delete(`/user/browse-history/${historyId}`);
}

export function deleteBrowseHistoryBatchApi(historyIds) {
    return http.post("/user/browse-history/delete-batch", historyIds);
}

export function clearBrowseHistoryApi() {
    return http.delete("/user/browse-history/all");
}
