<template>
  <div class="basic-layout">
    <el-container>
      <el-header>
        <div class="header-content">
          <div class="logo">
            <h2>宠物纪念馆</h2>
          </div>
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            router
            class="nav-menu"
          >
            <!-- 宠主端菜单 -->
            <template v-if="userStore.userRole === 0">
              <el-menu-item index="/pet-owner/service-packages">服务套餐</el-menu-item>
              <el-menu-item index="/pet-owner/book-service">预约服务</el-menu-item>
              <el-menu-item index="/pet-owner/service-progress">服务进度</el-menu-item>
              <el-menu-item index="/pet-owner/service-feedback">服务反馈</el-menu-item>
              <el-menu-item index="/memorial/list">我的纪念册</el-menu-item>
              <el-menu-item index="/pet-owner/grief-resources">哀伤资源</el-menu-item>
              <el-menu-item index="/pet-owner/urn-storage">骨灰寄存</el-menu-item>
            </template>
            <!-- 服务人员端菜单 -->
            <template v-else-if="userStore.userRole === 1">
              <el-menu-item index="/service-provider/appointment-list">预约列表</el-menu-item>
              <el-menu-item index="/service-provider/my-services">我的服务</el-menu-item>
              <el-menu-item index="/service-provider/schedule">日程安排</el-menu-item>
              <el-menu-item index="/service-provider/service-feedback">服务反馈</el-menu-item>
              <el-menu-item index="/service-provider/memorial-list">纪念册设计</el-menu-item>
              <el-menu-item index="/service-provider/template-management">模板管理</el-menu-item>
              <el-menu-item index="/service-provider/urn-storage">骨灰寄存管理</el-menu-item>
            </template>
            <!-- 管理员端菜单 -->
            <template v-else-if="userStore.userRole === 2">
              <el-menu-item index="/admin/dashboard">仪表盘</el-menu-item>
              <el-menu-item index="/admin/service-management">服务管理</el-menu-item>
              <el-menu-item index="/admin/template-management">模板管理</el-menu-item>
              <el-menu-item index="/admin/memorial-review">纪念册审核</el-menu-item>
              <el-menu-item index="/admin/user-management">用户管理</el-menu-item>
              <el-menu-item index="/admin/satisfaction-stats">满意度统计</el-menu-item>
              <el-menu-item index="/admin/special-requests">特殊需求管理</el-menu-item>
              <el-menu-item index="/admin/grief-resource">哀伤资源管理</el-menu-item>
              <el-menu-item index="/admin/urn-storage">骨灰寄存管理</el-menu-item>
            </template>
            <!-- 默认菜单 -->
            <template v-else>
              <el-menu-item index="/home">首页</el-menu-item>
              <el-menu-item index="/memorial/list">纪念册</el-menu-item>
              <el-menu-item index="/order/list">订单管理</el-menu-item>
              <el-menu-item index="/order/create">创建订单</el-menu-item>
            </template>
          </el-menu>
          <div class="user-info">
            <el-dropdown @command="handleCommand">
              <span class="el-dropdown-link">
                {{ userStore.username }}
                <el-icon class="el-icon--right">
                  <arrow-down />
                </el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile"
                    >个人信息</el-dropdown-item
                  >
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowDown } from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 当前激活的菜单
const activeMenu = computed(() => route.path);

// 初始化用户信息（每次组件挂载时刷新，确保从sessionStorage读取最新值）
onMounted(() => {
  // 每次挂载时都刷新用户信息，确保从sessionStorage读取最新值
  userStore.refreshUserInfo();
});

// 处理下拉菜单命令
const handleCommand = (command: string) => {
  if (command === "logout") {
    userStore.logout();
    ElMessage.success("已退出登录");
    router.push("/login");
  } else if (command === "profile") {
    // 跳转到个人信息页面
    router.push("/profile");
  }
};
</script>

<style scoped lang="scss">
.basic-layout {
  height: 100vh;

  .el-container {
    height: 100%;
  }

  .el-header {
    background-color: #fff;
    border-bottom: 1px solid #e4e7ed;
    padding: 0;

    .header-content {
      display: flex;
      align-items: center;
      height: 60px;
      padding: 0 20px;

      .logo {
        margin-right: 40px;

        h2 {
          margin: 0;
          color: #409eff;
        }
      }

      .nav-menu {
        flex: 1;
        border-bottom: none;
      }

      .user-info {
        margin-left: 20px;

        .el-dropdown-link {
          cursor: pointer;
          color: #606266;
          display: flex;
          align-items: center;
        }
      }
    }
  }

  .el-main {
    background-color: #f5f5f5;
    padding: 20px;
    overflow-y: auto;
  }
}
</style>
