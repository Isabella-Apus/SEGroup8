import { handleMockRequest } from "./handlers";

export async function mockRequest({ method, url, params, data, headers }) {
    // Simulate real network delay for test cases.
    await new Promise((resolve) => setTimeout(resolve, 120));
    return handleMockRequest({ method, url, params, data, headers });
}
