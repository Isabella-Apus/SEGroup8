import http from './http';

export function getMyReviewsApi(params = {}) {
    return http.get('/review/my', { params });
}

export function getSellerReviewsApi(params = {}) {
    return http.get('/review/seller/list', { params });
}

export function replyReviewApi(reviewId, payload) {
    return http.post(`/review/${reviewId}/reply`, payload);
}

export function followupReviewApi(payload) {
    return http.post('/review/followup', payload);
}
