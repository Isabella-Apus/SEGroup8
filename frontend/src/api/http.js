import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { mockRequest } from "@/mock-data";
import { API_BASE_URL } from "@/utils/url";
import { standardizeError } from "@/utils/errorStandard";

// 开发环境默认连真实后端；生产构建未配置时仍可用 mock。也可用 .env.development 显式设置 VITE_DATA_SOURCE。
const DATA_SOURCE = (
    import.meta.env.VITE_DATA_SOURCE ||
    (import.meta.env.DEV ? "real" : "mock")
).toLowerCase();

const realHttp = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
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
        if (data && data.code !== 0) {
            const message = standardizeError(data.message, data.code);
            ElMessage.error(message);
            const normalizedError = new Error(message);
            normalizedError.userMessage = message;
            normalizedError.response = { ...response, data: { ...data, message } };
            return Promise.reject(normalizedError);
        }
        return data;
    },
    (error) => {
        const userStore = useUserStore();
        const bizCode = error?.response?.data?.code;
        const statusCode = error?.response?.status;
        const message = standardizeError(
            error?.response?.data?.message || error.message || "Network error",
            bizCode || statusCode,
        );
        if (error?.response?.status === 401 || bizCode === 401) {
            userStore.logout();
        }
        error.userMessage = message;
        if (error.response?.data) {
            error.response.data.message = message;
        }
        ElMessage.error(message);
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
        const message = standardizeError(error?.message || "Mock request failed", error?.code);
        error.userMessage = message;
        ElMessage.error(message);
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
        if (DATA_SOURCE === "mock") {
            return mockAdapter("post", url, data, config);
        }
        return realHttp.post(url, data, config);
    },
    put(url, data = {}, config = {}) {
        if (DATA_SOURCE === "mock") {
            return mockAdapter("put", url, data, config);
        }
        return realHttp.put(url, data, config);
    },
    delete(url, config = {}) {
        if (DATA_SOURCE === "mock") {
            return mockAdapter("delete", url, undefined, config);
        }
        return realHttp.delete(url, config);
    },
};

export default http;
