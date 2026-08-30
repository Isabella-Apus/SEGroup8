import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { mockRequest } from "@/mock-data";
import { API_BASE_URL } from "@/utils/url";

// 默认连接真实后端；只有显式设置 VITE_DATA_SOURCE=mock 时才启用演示数据。
const DATA_SOURCE = (
    import.meta.env.VITE_DATA_SOURCE ||
    "real"
).toLowerCase();

function createIdempotencyKey() {
    if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
    return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function withIdempotency(method, url, config = {}) {
    if (!["post", "put", "delete"].includes(method.toLowerCase())) return config;
    if (!/^\/(order|review|logistics|admin\/orders)(\/|$)/.test(url)) return config;
    const headers = { ...(config.headers || {}) };
    if (!headers["Idempotency-Key"]) {
        headers["Idempotency-Key"] = config.idempotencyKey || createIdempotencyKey();
    }
    return { ...config, headers };
}

const realHttp = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
});

realHttp.interceptors.request.use(
    (config) => {
        const userStore = useUserStore();
        if (userStore.token) {
            config.headers.Authorization = `Bearer ${userStore.token}`;
        }
        return config;
    },
    (error) => Promise.reject(error),
);

realHttp.interceptors.response.use(
    (response) => {
        const { data } = response;
        if (data && Object.prototype.hasOwnProperty.call(data, "code") && data.code !== 0) {
            const message = data.message || "Request failed";
            if (!response.config?.silent) {
                ElMessage.error(message);
            }
            return Promise.reject(new Error(message));
        }
        return data;
    },
    (error) => {
        const userStore = useUserStore();
        const message =
            error?.response?.data?.message || error.message || "Network error";
        const bizCode = error?.response?.data?.code;
        if (error?.response?.status === 401 || bizCode === 401) {
            userStore.logout();
        }
        if (!error?.config?.silent) {
            ElMessage.error(message);
        }
        return Promise.reject(error);
    },
);

async function mockAdapter(method, url, data, config) {
    try {
        const userStore = useUserStore();
        const headers = {
            ...(config?.headers || {}),
        };
        if (userStore.token) {
            headers.Authorization = `Bearer ${userStore.token}`;
        }
        return await mockRequest({
            method,
            url,
            params: config?.params,
            data,
            headers,
        });
    } catch (error) {
        const message = error?.message || "Mock request failed";
        if (!config?.silent) {
            ElMessage.error(message);
        }
        return Promise.reject(error);
    }
}

const http = {
    get(url, config = {}) {
        if (DATA_SOURCE === "mock") {
            return mockAdapter("get", url, undefined, config);
        }
        return realHttp.get(url, config);
    },
    post(url, data = {}, config = {}) {
        config = withIdempotency("post", url, config);
        if (DATA_SOURCE === "mock") {
            return mockAdapter("post", url, data, config);
        }
        return realHttp.post(url, data, config);
    },
    put(url, data = {}, config = {}) {
        config = withIdempotency("put", url, config);
        if (DATA_SOURCE === "mock") {
            return mockAdapter("put", url, data, config);
        }
        return realHttp.put(url, data, config);
    },
    delete(url, config = {}) {
        config = withIdempotency("delete", url, config);
        if (DATA_SOURCE === "mock") {
            return mockAdapter("delete", url, undefined, config);
        }
        return realHttp.delete(url, config);
    },
};

export default http;
