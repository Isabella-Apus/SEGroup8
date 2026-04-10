import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { mockRequest } from "@/mock-data";

// 开发环境默认连真实后端；生产构建未配置时仍可用 mock。也可用 .env.development 显式设置 VITE_DATA_SOURCE。
const DATA_SOURCE = (
    import.meta.env.VITE_DATA_SOURCE ||
    (import.meta.env.DEV ? "real" : "mock")
).toLowerCase();

const realHttp = axios.create({
    baseURL: "http://localhost:8080/api",
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
            const message = data.message || "Request failed";
            ElMessage.error(message);
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
        const message = error?.message || "Mock request failed";
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
