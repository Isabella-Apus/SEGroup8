import http from "./http";

export function pageUsersApi(params) {
    return http.get("/admin/users", { params });
}

export function banUserApi(userId) {
    return http.put(`/admin/users/${userId}/ban`);
}

export function unbanUserApi(userId) {
    return http.put(`/admin/users/${userId}/unban`);
}
