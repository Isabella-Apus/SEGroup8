import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "@/assets/styles/global.css";
import { useUserStore } from "@/stores/user";
import { startRealtimeClient } from "@/realtime/realtimeClient";
import { startRealtimeNotifier } from "@/realtime/realtimeNotifier";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(ElementPlus);

const userStore = useUserStore();
userStore.restore();
if (userStore.isLoggedIn) {
  startRealtimeClient();
}
startRealtimeNotifier(router);

app.use(router);
app.mount("#app");
