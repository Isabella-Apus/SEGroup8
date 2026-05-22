const CART_KEY = "segroup8_secondhand_cart_items";

function readRaw() {
  const value = localStorage.getItem(CART_KEY);
  if (!value) {
    return [];
  }
  try {
    const data = JSON.parse(value);
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

function writeRaw(items) {
  localStorage.setItem(CART_KEY, JSON.stringify(items));
}

export function getSecondhandCartItems() {
  return readRaw();
}

export function saveSecondhandCartItems(items) {
  writeRaw(items);
}

export function addSecondhandToCart(item) {
  const items = readRaw();
  const productId = item.id || item.productId;
  const exists = items.some((cartItem) => Number(cartItem.productId) === Number(productId));
  if (!exists) {
    items.push({
      productId,
      name: item.name,
      cover: item.cover,
      price: item.salePrice ?? item.price,
      originPrice: item.originPrice,
      conditionLevel: item.conditionLevel || item.condition,
      sellerUserId: item.sellerUserId,
      sellerName: item.sellerName,
    });
  }
  writeRaw(items);
  return items;
}

export function removeSecondhandFromCart(productId) {
  const items = readRaw().filter((item) => Number(item.productId) !== Number(productId));
  writeRaw(items);
  return items;
}

export function clearSecondhandCart() {
  writeRaw([]);
}
