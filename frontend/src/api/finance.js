import http from "./http";

export function getFinanceDashboardApi() {
    return http.get("/finance/dashboard");
}

export function getMyWalletRecordsApi() {
    return http.get("/finance/my-wallet/records");
}

export function getBusinessRecordsApi() {
    return http.get("/finance/business/records");
}

export function rechargeCoinApi(payload) {
    return http.post("/finance/recharge", payload);
}
