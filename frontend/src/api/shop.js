import http from "./http";

export function getPublicShopApi(shopId) {
  return http.get(`/shop/public/${shopId}`);
}

export function getPublicShopProductsApi(shopId, params = {}) {
  return http.get(`/shop/public/${shopId}/products`, { params });
}

export function getCurrentSellerShopApi() {
  return http.get("/shop/seller/current");
}

export function saveShopDecorationApi(decoration) {
  const decorationJson = typeof decoration === "string"
    ? decoration
    : JSON.stringify(decoration || {});
  return http.put("/shop/seller/decoration", { decorationJson });
}
