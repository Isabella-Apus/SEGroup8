import http from "./http";

export function getSellerReviewListApi(params = {}) {
    return http.get("/review/seller/list", { params });
}

export function replyReviewApi(reviewId, payload) {
    return http.post(`/review/${reviewId}/reply`, payload);
}

