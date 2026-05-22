import { handleMockRequest } from "./handlers";
import { persistMockStore, syncMockStoreFromStorage } from "./store";

export async function mockRequest({ method, url, params, data, headers }) {
    // Simulate real network delay for test cases.
    await new Promise((resolve) => setTimeout(resolve, 120));
    syncMockStoreFromStorage();
    const result = await handleMockRequest({ method, url, params, data, headers });
    if (String(method || "get").toLowerCase() !== "get") {
        persistMockStore();
    }
    return result;
}
