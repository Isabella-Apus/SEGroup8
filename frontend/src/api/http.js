import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const http = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
});

http.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

http.interceptors.response.use(
  (response) => {
    const { data } = response;
    if (data && data.code !== 0) {
      const message = data.message || 'Request failed';
      ElMessage.error(message);
      return Promise.reject(new Error(message));
    }
    return data;
  },
  (error) => {
    const authStore = useAuthStore();
    const message = error?.response?.data?.message || error.message || 'Network error';
    if (error?.response?.status === 401) {
      authStore.logout();
    }
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default http;
