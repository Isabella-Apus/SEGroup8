import { defineStore } from "pinia";
import { loginApi, registerApi } from "@/api/auth";
import { getProfileApi } from "@/api/user";
import {
    clearToken,
    clearUser,
    getToken,
    getUser,
    setToken,
    setUser,
} from "@/utils/storage";
import { startRealtimeClient, stopRealtimeClient } from "@/realtime/realtimeClient";

export const useUserStore = defineStore("user", {
    state: () => ({
        token: "",
        userInfo: null,
        role: "",
        isLoggedIn: false,
    }),
    getters: {
        currentRole: (state) => state.role || state.userInfo?.role || "",
    },
    actions: {
        restore() {
            this.token = getToken();
            this.userInfo = getUser();
            this.role = this.userInfo?.role || "";
            this.isLoggedIn = !!this.token;
        },
        async login(loginForm) {
            const result = await loginApi(loginForm);
            this.token = result.data.token;
            this.userInfo = result.data.user;
            this.role = result.data.role || result.data.user?.role || "";
            this.isLoggedIn = true;
            setToken(this.token);
            setUser(this.userInfo);
            startRealtimeClient();
            return this.userInfo;
        },
        async register(registerForm) {
            await registerApi(registerForm);
        },
        async fetchProfile() {
            if (!this.token) {
                return null;
            }
            const result = await getProfileApi();
            this.userInfo = result.data;
            this.role = result.data?.role || "";
            this.isLoggedIn = !!this.token;
            setUser(this.userInfo);
            return this.userInfo;
        },
        logout() {
            this.token = "";
            this.userInfo = null;
            this.role = "";
            this.isLoggedIn = false;
            clearToken();
            clearUser();
            stopRealtimeClient();
        },
    },
});
