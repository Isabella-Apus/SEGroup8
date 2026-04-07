import http from "./http";

export function loginApi(payload) {
    return http.post("/auth/login", payload);
}

export function registerApi(payload) {
    return http.post("/auth/register", payload);
}

export async function getCurrentUserApi() {
    try {
        return await http.get("/user/me");
    } catch (error) {
        return http.get("/user/profile");
    }
}
