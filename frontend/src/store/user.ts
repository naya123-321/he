import { defineStore } from "pinia";
import { getUserInfo, type UserVO } from "@/api/user";

// 从sessionStorage读取数据的辅助函数
const getTokenFromStorage = () => {
  try {
    return sessionStorage.getItem("token") || "";
  } catch {
    return "";
  }
};

const getUserInfoFromStorage = (): UserVO | null => {
  try {
    const userInfoStr = sessionStorage.getItem("userInfo");
    return userInfoStr ? JSON.parse(userInfoStr) : null;
  } catch {
    return null;
  }
};

export const useUserStore = defineStore("user", {
  state: () => ({
    // 使用sessionStorage代替localStorage，让每个标签页独立存储
    token: getTokenFromStorage(),
    userInfo: getUserInfoFromStorage(),
    permissions: [] as string[],
  }),

  getters: {
    // 每次访问时都从sessionStorage读取最新值，确保多标签页独立
    isLogin: () => {
      return !!getTokenFromStorage();
    },
    username: () => {
      const userInfo = getUserInfoFromStorage();
      return userInfo?.username || "";
    },
    userName: () => {
      const userInfo = getUserInfoFromStorage();
      return userInfo?.username || "";
    },
    userId: () => {
      const userInfo = getUserInfoFromStorage();
      return userInfo?.id || null;
    },
    userRole: () => {
      const userInfo = getUserInfoFromStorage();
      return userInfo?.role || 0;
    },
  },

  actions: {
    // 设置token
    setToken(token: string) {
      this.token = token;
      try {
        sessionStorage.setItem("token", token);
      } catch (error) {
        console.error("保存token失败:", error);
      }
    },

    // 清除token
    clearToken() {
      this.token = "";
      this.userInfo = null;
      try {
        sessionStorage.removeItem("token");
        sessionStorage.removeItem("userInfo");
      } catch (error) {
        console.error("清除token失败:", error);
      }
    },

    // 设置用户信息
    setUserInfo(userInfo: UserVO) {
      this.userInfo = userInfo;
      try {
        sessionStorage.setItem("userInfo", JSON.stringify(userInfo));
      } catch (error) {
        console.error("保存用户信息失败:", error);
      }
    },

    // 获取用户信息
    async getUserInfo() {
      try {
        const res = await getUserInfo();
        if (res.code === 200) {
          this.setUserInfo(res.data);
          return res.data;
        }
      } catch (error) {
        this.clearToken();
        throw error;
      }
    },

    // 初始化用户信息（从本地存储）
    initUserInfo() {
      // 从sessionStorage重新读取，确保获取最新值
      this.token = getTokenFromStorage();
      this.userInfo = getUserInfoFromStorage();
    },

    // 刷新用户信息（从sessionStorage同步到store）
    refreshUserInfo() {
      this.token = getTokenFromStorage();
      this.userInfo = getUserInfoFromStorage();
    },

    // 退出登录
    logout() {
      this.clearToken();
      // 跳转到登录页
      window.location.href = "/login";
    },
  },
});
