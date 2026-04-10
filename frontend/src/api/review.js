import http from "./http";

export function getMyReviewListApi(params = {}) {
  return http.get("/review/my", { params });
}

export function submitFollowUpReviewApi(payload) {
  return http.post("/review/followup", payload);
}

