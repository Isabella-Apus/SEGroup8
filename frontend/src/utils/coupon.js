export function getCouponEligibleAmount(coupon, totalAmount, shopAmounts = new Map()) {
  const total = Number(totalAmount || 0);
  const isPlatform = Number(coupon?.scopeType) === 2 || Number(coupon?.voucherType) === 2;
  if (isPlatform || !coupon?.shopId) {
    return total;
  }
  return Number(shopAmounts.get(Number(coupon.shopId)) || 0);
}

export function getCouponDiscount(coupon, totalAmount, shopAmounts = new Map()) {
  if (!coupon) {
    return 0;
  }
  const eligibleAmount = getCouponEligibleAmount(coupon, totalAmount, shopAmounts);
  if (eligibleAmount < Number(coupon.minAmount || 0)) {
    return 0;
  }
  if (Number(coupon.type) === 1) {
    return Math.min(eligibleAmount, Number(coupon.discountAmount || 0));
  }
  if (Number(coupon.type) === 2) {
    return Math.max(0, eligibleAmount * (1 - Number(coupon.discountRate || 1)));
  }
  return 0;
}

export function formatCouponOption(coupon) {
  const benefit = Number(coupon?.type) === 2
    ? `${Number(coupon.discountRate || 1) * 10}折`
    : `减¥${Number(coupon?.discountAmount || 0).toFixed(2)}`;
  const threshold = Number(coupon?.minAmount || 0) > 0
    ? `满¥${Number(coupon.minAmount).toFixed(2)}`
    : "无门槛";
  return `${coupon?.name || "优惠券"}（${threshold}，${benefit}）`;
}
