import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";

const http = axios.create({
    baseURL: "http://localhost:8080/api",
    timeout: 10000,
});

http.interceptors.request.use(
    (config) => {
        const userStore = useUserStore();
        if (userStore.token) {
            config.headers.Authorization = `Bearer ${userStore.token}`;
        }
        return config;
    },
    (error) => Promise.reject(error),
);

http.interceptors.response.use(
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

export default http;
