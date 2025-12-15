import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

// 创建axios实例
const service = axios.create({
  baseURL: "/api",
  timeout: 30000, // 增加超时时间到30秒，避免长时间加载
});

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 使用sessionStorage，让每个标签页独立存储token
    const token = sessionStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // 仅在非 FormData 情况下设置 JSON Content-Type，避免破坏 multipart 边界
    const isFormData = typeof FormData !== "undefined" && config.data instanceof FormData;
    if (config.data && !isFormData && !config.headers["Content-Type"]) {
      config.headers["Content-Type"] = "application/json";
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      ElMessage.error(res.message || "请求失败");
      return Promise.reject(new Error(res.message || "Error"));
    }
    return res;
  },
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem("token");
      sessionStorage.removeItem("userInfo");
      router.push("/login");
      ElMessage.error("登录已过期，请重新登录");
    } else if (error.response?.status === 404) {
      const url = error.config?.url || '';
      const message = error.response?.data?.message || error.message || `接口不存在: ${url}`;
      console.error("API请求失败:", {
        url: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
        message: message
      });
      ElMessage.error(`请求失败: ${message}`);
    } else {
      const message = error.response?.data?.message || error.message || "网络错误";
      console.error("请求错误:", error);
      ElMessage.error(message);
    }
    return Promise.reject(error);
  }
);

export default service;
