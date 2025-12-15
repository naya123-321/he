import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import "./assets/css/main.scss";
import { useUserStore } from "./store/user";

const app = createApp(App);
const pinia = createPinia();

// 注册所有Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(pinia);

// 初始化用户信息（每个标签页独立初始化）
const userStore = useUserStore();
userStore.initUserInfo();

// 监听storage事件，当其他标签页修改sessionStorage时同步（虽然sessionStorage不触发storage事件，但保留此逻辑以防未来需要）
// 注意：sessionStorage不会触发storage事件，只有localStorage会
// 所以这里主要是确保每个标签页独立初始化

app.use(router);
app.use(ElementPlus);

app.mount("#app");

