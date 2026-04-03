import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "@/assets/styles/global.css";
import { useUserStore } from "@/stores/user";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const userStore = useUserStore();
userStore.restore();

app.use(router);
app.use(ElementPlus);
app.mount("#app");
