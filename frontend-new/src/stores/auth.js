import { defineStore } from 'pinia';
import { getCurrentUserApi, loginApi } from '@/api/auth';
import { clearToken, clearUser, getToken, getUser, setToken, setUser } from '@/utils/storage';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.user?.role || 'USER'
  },
  actions: {
    restore() {
      this.token = getToken();
      this.user = getUser();
    },
    async login(loginForm) {
      const result = await loginApi(loginForm);
      this.token = result.data.token;
      this.user = result.data.user;
      setToken(this.token);
      setUser(this.user);
    },
    async fetchMe() {
      if (!this.token) {
        return;
      }
      const result = await getCurrentUserApi();
      this.user = result.data;
      setUser(this.user);
    },
    logout() {
      this.token = '';
      this.user = null;
      clearToken();
      clearUser();
    }
  }
});
