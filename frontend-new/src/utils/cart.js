const CART_KEY = 'segroup8_cart_items';

function readRaw() {
    const value = localStorage.getItem(CART_KEY);
    if (!value) {
        return [];
    }
    try {
        const data = JSON.parse(value);
        return Array.isArray(data) ? data : [];
    } catch (error) {
        return [];
    }
}

function writeRaw(items) {
    localStorage.setItem(CART_KEY, JSON.stringify(items));
}

export function getCartItems() {
    return readRaw();
}

export function saveCartItems(items) {
    writeRaw(items);
}

export function addToCart(product, quantity, options = {}) {
    const itemType = options.itemType || 'PRODUCT';
    const unitPrice = options.unitPrice ?? (itemType === 'SECONDHAND' ? product.salePrice : product.price);
    const stock = options.stock ?? product.stock;
    const items = readRaw();
    const index = items.findIndex(
        (item) => item.productId === product.id && (item.itemType || 'PRODUCT') === itemType
    );
    if (index >= 0) {
        items[index].quantity += quantity;
    } else {
        items.push({
            productId: product.id,
            itemType,
            name: product.name,
            cover: product.cover,
            price: unitPrice,
            quantity,
            stock
        });
    }
    writeRaw(items);
    return items;
}

export function removeFromCart(productId, itemType = 'PRODUCT') {
    const items = readRaw().filter(
        (item) => !(item.productId === productId && (item.itemType || 'PRODUCT') === itemType)
    );
    writeRaw(items);
    return items;
}

export function clearCart() {
    writeRaw([]);
}
