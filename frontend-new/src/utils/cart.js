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

export function addToCart(product, quantity) {
    const items = readRaw();
    const index = items.findIndex((item) => item.productId === product.id);
    if (index >= 0) {
        items[index].quantity += quantity;
    } else {
        items.push({
            productId: product.id,
            name: product.name,
            cover: product.cover,
            price: product.price,
            quantity,
            stock: product.stock
        });
    }
    writeRaw(items);
    return items;
}

export function removeFromCart(productId) {
    const items = readRaw().filter((item) => item.productId !== productId);
    writeRaw(items);
    return items;
}

export function clearCart() {
    writeRaw([]);
}
