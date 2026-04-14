import http from './http'

// ===== 商品管理 =====
export function getMyProducts(params) {
    return http.get('/product/seller/list', { params })
}
export function getProductDetail(id) {
    return http.get(`/product/detail/${id}`)
}
export function createProduct(data) {
    return http.post('/product/seller', data)
}
export function updateProduct(id, data) {
    return http.put(`/product/seller/${id}`, data)
}
export function deleteProduct(id) {
    return http.delete(`/product/seller/${id}`)
}
export function updateProductStatus(id, status) {
    return http.post(`/product/seller/${id}/status`, { status })
}
export function adjustProductStock(id, delta) {
    return http.post(`/product/seller/${id}/stock/adjust`, { delta })
}

// ===== 订单管理 =====
export function getMyOrders(params) {
    return http.get('/order/seller/list', { params })
}
export function getOrderDetail(id) {
    return http.get(`/order/seller/detail/${id}`)
}
export function shipOrder(id) {
    return http.post(`/order/${id}/ship`)
}
export function approveRefund(id) {
    return http.post(`/order/${id}/refund/approve`)
}
export function rejectRefund(id) {
    return http.post(`/order/${id}/refund/reject`)
}

// ===== 评价管理 =====
export function getMyReviews(params) {
    return http.get('/review/my', { params })
}
export function replyReview(reviewId, content) {
    return http.post(`/review/seller/${reviewId}/reply`, { content })
}

// ===== 图片上传 =====
export async function uploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    const res = await http.post('/upload/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
    // 拼上后端地址
    res.data.url = 'http://localhost:8080' + res.data.url
    return res
}

// ===== 优惠券管理 =====
export function getMyVouchers(params) {
    return http.get('/voucher/seller/list', { params })
}
export function createVoucher(data) {
    return http.post('/voucher/seller', data)
}
export function updateVoucher(id, data) {
    return http.put(`/voucher/seller/${id}`, data)
}
export function closeVoucher(id) {
    return http.post(`/voucher/seller/${id}/close`)
}
export function deleteVoucher(id) {
    return http.delete(`/voucher/seller/${id}`)
}
// ===== 店铺设置 =====
export function updateShopProfile(data) {
    return http.put('/user/profile', data)
}