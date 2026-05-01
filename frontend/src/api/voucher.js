import http from './http'

export function pageAvailableVoucherApi(params = {}) {
  return http.get('/voucher/list', { params })
}

export function claimVoucherApi(id) {
  return http.post(`/voucher/${id}/claim`)
}

export function myVoucherApi(params = {}) {
  return http.get('/voucher/my', { params })
}

export function myAvailableVoucherApi(params = {}) {
  return http.get('/voucher/my/available', { params })
}

export function myUnavailableVoucherReasonsApi(params = {}) {
  return http.get('/voucher/my/available/reasons', { params })
}
