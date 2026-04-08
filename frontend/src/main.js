import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "@/assets/styles/global.css";
import { useUserStore } from "@/stores/user";
import { startRealtimeClient } from "@/realtime/realtimeClient";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const userStore = useUserStore();
userStore.restore();
if (userStore.isLoggedIn) {
  startRealtimeClient();
}

app.use(router);
app.mount("#app");
