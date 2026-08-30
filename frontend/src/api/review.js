import http from "./http";

export function getMyReviewListApi(params = {}) {
  return http.get("/review/my", { params });
}

export function submitFollowUpReviewApi(payload) {
  return http.post("/review/followup", payload);
}

export function getSellerReviewsApi(params = {}) {
  return http.get("/review/seller/list", { params });
}

export function replyReviewApi(reviewId, payload) {
  return http.post(`/review/${reviewId}/reply`, payload);
}

export const getMyReviewsApi = getMyReviewListApi;
export const followupReviewApi = submitFollowUpReviewApi;
