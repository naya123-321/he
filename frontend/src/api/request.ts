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
    // 如果请求被取消，不显示错误
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }

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
      // 对于404错误，只在控制台记录，不显示给用户（避免首页加载时显示过多错误）
      if (!url.includes('/enabled')) {
        ElMessage.error(`请求失败: ${message}`);
      }
    } else if (error.response?.status === 500) {
      const message = error.response?.data?.message || error.message || "服务器内部错误";
      console.error("服务器错误:", {
        url: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
        message: message,
        error: error
      });
      // 对于首页的公开接口（模板和服务类型），静默失败，不显示错误
      // 因为这些接口失败不应该影响首页的正常显示
      const url = error.config?.url || '';
      if (url.includes('/template/enabled') || url.includes('/service-type/enabled')) {
        console.warn("首页数据加载失败，使用默认数据:", url);
        // 不显示错误消息，让页面正常显示
      } else {
        ElMessage.error(message);
      }
    } else {
      const message = error.response?.data?.message || error.message || "网络错误";
      console.error("请求错误:", error);
      ElMessage.error(message);
    }
    return Promise.reject(error);
  }
);

export default service;
