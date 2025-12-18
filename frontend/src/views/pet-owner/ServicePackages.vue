<template>
  <div class="service-packages-container">
    <el-page-header @back="goBack" content="选择服务套餐" />

    <div class="packages-intro">
      <h2>专业的宠物殡葬服务</h2>
      <p>我们提供多种服务套餐，根据您的需求和预算选择最适合的方案</p>
    </div>

    <!-- 智能套餐推荐（访客无需登录） -->
    <el-card class="smart-recommend-card" shadow="hover">
      <template #header>
        <div class="smart-header">
          <div class="left">
            <div class="title">智能套餐推荐</div>
            <div class="sub">基于协同过滤 + 规则引擎</div>
          </div>
          <div class="right">
            <el-button size="small" @click="goVisitorPetInfo">
              {{ visitorProfile ? "修改信息" : "填写信息" }}
            </el-button>
            <el-button v-if="visitorProfile" size="small" @click="clearVisitorProfile">清除</el-button>
          </div>
        </div>
      </template>

      <div v-if="!visitorProfile" class="smart-empty">
        <el-empty description="填写宠物信息后可获得更贴合的套餐推荐" />
      </div>

      <div v-else class="smart-body" v-loading="recoLoading">
        <div class="smart-line">
          根据您宠物的信息，为您推荐最适合的套餐：
        </div>
        <div class="smart-reco">
          <span class="star">⭐</span>
          <span class="label">推荐套餐：</span>
          <span class="name">{{ recommendation?.recommendedPackageName || "（计算中）" }}</span>
          <el-tag v-if="recommendation?.score != null" type="success" class="score">
            推荐度{{ Math.round(recommendation.score) }}%
          </el-tag>
        </div>

        <div class="smart-section" v-if="recommendation?.analysis?.length">
          <div class="sec-title">📊 匹配分析：</div>
          <ul class="sec-list">
            <li v-for="(it, idx) in recommendation.analysis" :key="idx">• {{ it }}</li>
          </ul>
        </div>

        <div class="smart-section">
          <div class="sec-title">🔍 推荐算法：</div>
          <div class="sec-text">
            {{ recommendation?.algorithm || "基于协同过滤算法（相似用户）+ 简单规则引擎（特征匹配）" }}
            <span v-if="recommendation?.similarUsers != null">
              ，分析{{ recommendation.similarUsers }}个相似用户的选择
            </span>
          </div>
          <el-alert
            v-if="recommendation?.warning"
            :title="recommendation.warning"
            type="warning"
            show-icon
            class="warn"
          />
        </div>

        <div class="smart-actions">
          <el-button type="primary" @click="reloadRecommendation" :loading="recoLoading">
            重新计算推荐
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-loading="loading" class="packages-grid">
      <el-card
        v-for="pkg in servicePackages"
        :key="pkg.id"
        :class="{ 'selected-package': selectedPackageId === pkg.id }"
        class="package-card"
        shadow="hover"
        @click="selectPackage(pkg)"
      >
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <h3>{{ pkg.name }}</h3>
              <el-tag
                :type="isSmartRecommended(pkg) ? 'success' : (isDbRecommended(pkg) ? 'warning' : 'info')"
                size="small"
                class="recommend-tag"
              >
                {{ isSmartRecommended(pkg) ? "智能推荐" : (isDbRecommended(pkg) ? "推荐" : "标准") }}
              </el-tag>
            </div>
            <el-button
              type="text"
              size="small"
              @click.stop="viewPackageDetail(pkg)"
              class="detail-btn"
            >
              <el-icon><View /></el-icon>
              详情
            </el-button>
          </div>
        </template>

        <div class="package-content">
          <div class="package-price">
            <span class="price">¥{{ pkg.price?.toFixed(2) || '0.00' }}</span>
            <span class="unit" v-if="pkg.duration">/{{ pkg.duration }}分钟</span>
          </div>

          <div class="package-description" v-if="pkg.description">
            {{ pkg.description }}
          </div>

          <div class="package-features-preview" v-if="pkg.process">
            <ul class="features-list">
              <li v-for="(feature, index) in parseProcessToFeatures(pkg.process).slice(0, 3)" :key="index">
                <el-icon><Check /></el-icon>
                <span>{{ feature }}</span>
              </li>
              <li v-if="parseProcessToFeatures(pkg.process).length > 3" class="more-features">
                <el-icon><MoreFilled /></el-icon>
                <span>还有{{ parseProcessToFeatures(pkg.process).length - 3 }}项服务</span>
              </li>
            </ul>
          </div>

          <div class="card-footer">
            <el-button
              type="primary"
              :class="{ 'selected-btn': selectedPackageId === pkg.id }"
              @click.stop="selectPackage(pkg)"
              style="width: 100%"
            >
              {{ selectedPackageId === pkg.id ? '已选择' : '选择此套餐' }}
            </el-button>
          </div>
        </div>
      </el-card>
      
      <!-- 空状态 -->
      <el-empty 
        v-if="!loading && servicePackages.length === 0" 
        description="暂无可用服务套餐"
        style="grid-column: 1 / -1;"
      />
    </div>

    <div class="action-buttons" v-if="selectedPackageId">
      <el-button @click="goBack">取消</el-button>
      <el-button
        type="primary"
        size="large"
        @click="goToBookService"
      >
        {{ isLoggedIn ? "下一步，预约服务" : "登录后预约" }}
      </el-button>
    </div>

    <!-- 套餐详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="currentPackage?.name"
      width="700px"
      class="package-detail-dialog"
    >
      <div v-if="currentPackage" class="package-detail-content">
        <div class="detail-header">
          <div class="detail-price">
            <span class="price-large">¥{{ currentPackage.price?.toFixed(2) || '0.00' }}</span>
            <span class="unit-large" v-if="currentPackage.duration">/{{ currentPackage.duration }}分钟</span>
          </div>
          <el-tag
            :type="isRecommended(currentPackage) ? 'success' : 'info'"
            size="large"
          >
            {{ isRecommended(currentPackage) ? "推荐套餐" : "标准套餐" }}
          </el-tag>
        </div>

        <div class="detail-section" v-if="currentPackage.description">
          <h4>套餐介绍</h4>
          <p>{{ currentPackage.description }}</p>
        </div>

        <div class="detail-section" v-if="currentPackage.process">
          <h4>服务内容</h4>
          <ul class="detail-features">
            <li v-for="(feature, index) in parseProcessToFeatures(currentPackage.process)" :key="index">
              <el-icon><Check /></el-icon>
              <span>{{ feature }}</span>
            </li>
          </ul>
        </div>

        <div class="detail-section" v-if="currentPackage.process">
          <h4>服务流程</h4>
          <div class="process-content">
            {{ currentPackage.process }}
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          type="primary"
          @click="selectFromDetail"
        >
          选择此套餐
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElEmpty, ElDialog } from "element-plus";
import { Check, View, MoreFilled } from "@element-plus/icons-vue";
import { serviceTypeApi, type ServiceTypeVO } from "@/api/order";
import { recommendationApi, type VisitorPetProfile, type PackageRecommendationResult } from "@/api/recommendation";

const router = useRouter();
const route = useRoute();
const selectedPackageId = ref<number | null>(null);
const selectedPackage = ref<ServiceTypeVO | null>(null);
const loading = ref(false);
const detailDialogVisible = ref(false);
const currentPackage = ref<ServiceTypeVO | null>(null);

// 服务套餐数据（从API获取）
const servicePackages = ref<ServiceTypeVO[]>([]);

const isLoggedIn = !!sessionStorage.getItem("token");

const visitorProfile = ref<VisitorPetProfile | null>(null);
const recommendation = ref<PackageRecommendationResult | null>(null);
const recoLoading = ref(false);

const VISITOR_PROFILE_KEY = "visitorPetProfile";

// 将服务流程字符串转换为特性列表
const parseProcessToFeatures = (process?: string): string[] => {
  if (!process) return [];
  // 按换行或分号分割，然后清理每个项目
  return process
    .split(/[\n;]/)
    .map(item => item.trim())
    .filter(item => item.length > 0)
    .slice(0, 8); // 最多显示8个特性
};

// 将服务流程字符串转换为服务列表
const parseProcessToServices = (process?: string): Array<{ id: number; name: string }> => {
  if (!process) return [];
  return parseProcessToFeatures(process).map((item, index) => ({
    id: index + 1,
    name: item,
  }));
};

// 判断是否推荐（数据库字段）
const isDbRecommended = (pkg: ServiceTypeVO): boolean => {
  // 使用数据库中的 isRecommended 字段
  return pkg.isRecommended === true;
};

// 判断是否“智能推荐”
const isSmartRecommended = (pkg: ServiceTypeVO): boolean => {
  return !!recommendation.value?.recommendedPackageId && pkg.id === recommendation.value?.recommendedPackageId;
};

// 返回上一页
const goBack = () => {
  router.back();
};

const goVisitorPetInfo = () => {
  router.push("/visitor/pet-info");
};

const clearVisitorProfile = () => {
  sessionStorage.removeItem(VISITOR_PROFILE_KEY);
  visitorProfile.value = null;
  recommendation.value = null;
  ElMessage.success("已清除访客信息");
};

// 选择套餐
const selectPackage = (packageData: ServiceTypeVO) => {
  selectedPackageId.value = packageData.id || null;
  selectedPackage.value = packageData;
  ElMessage.success(`已选择套餐：${packageData.name}`);
};

// 查看套餐详情
const viewPackageDetail = (packageData: ServiceTypeVO) => {
  currentPackage.value = packageData;
  detailDialogVisible.value = true;
};

// 从详情对话框选择套餐
const selectFromDetail = () => {
  if (currentPackage.value) {
    selectPackage(currentPackage.value);
    detailDialogVisible.value = false;
  }
};

// 跳转到预约服务页面
const goToBookService = () => {
  if (!selectedPackage.value) {
    ElMessage.warning("请选择一个服务套餐");
    return;
  }

  if (!isLoggedIn) {
    ElMessage.info("请先登录后再预约服务");
    const redirect = `/pet-owner/book-service?packageId=${encodeURIComponent(
      String(selectedPackageId.value)
    )}&packageName=${encodeURIComponent(selectedPackage.value.name || "")}`;
    router.push({ path: "/login", query: { redirect } });
    return;
  }

  // 将选中的套餐信息传递给预约页面
  router.push({
    path: "/pet-owner/book-service",
    query: {
      packageId: String(selectedPackageId.value),
      packageName: selectedPackage.value.name,
    },
  });
};

const applySmartSelection = () => {
  const rid = recommendation.value?.recommendedPackageId;
  if (!rid) return;
  const pkg = servicePackages.value.find(p => p.id === rid);
  if (!pkg) return;
  selectedPackageId.value = pkg.id || null;
  selectedPackage.value = pkg;
};

const loadVisitorProfile = () => {
  const raw = sessionStorage.getItem(VISITOR_PROFILE_KEY);
  if (!raw) {
    visitorProfile.value = null;
    return;
  }
  try {
    const v = JSON.parse(raw) as VisitorPetProfile;
    if (v && typeof v === "object") {
      visitorProfile.value = {
        petType: v.petType || "",
        petAge: Number.isFinite(v.petAge) ? v.petAge : 0,
        deathCause: v.deathCause || "",
        budgetMin: (v as any).budgetRange?.[0],
        budgetMax: (v as any).budgetRange?.[1],
        participants: (v as any).participants,
      };
    }
  } catch {
    visitorProfile.value = null;
  }
};

const loadRecommendation = async () => {
  if (!visitorProfile.value) return;
  recoLoading.value = true;
  try {
    const res: any = await recommendationApi.getPackageRecommendation({
      petType: visitorProfile.value.petType,
      petAge: visitorProfile.value.petAge,
      deathCause: visitorProfile.value.deathCause,
      budgetMin: visitorProfile.value.budgetMin,
      budgetMax: visitorProfile.value.budgetMax,
      participants: visitorProfile.value.participants,
    });
    recommendation.value = res?.data || null;
    applySmartSelection();
  } catch (e: any) {
    recommendation.value = {
      warning: e?.message || "推荐服务暂不可用，请稍后重试",
    };
  } finally {
    recoLoading.value = false;
  }
};

const reloadRecommendation = async () => {
  await loadRecommendation();
  if (recommendation.value?.recommendedPackageId) {
    const pkg = servicePackages.value.find(p => p.id === recommendation.value?.recommendedPackageId);
    if (pkg) {
      ElMessage.success(`已为您标记智能推荐：${pkg.name}`);
    }
  }
};

// 加载服务套餐列表
const loadServicePackages = async () => {
  loading.value = true;
  try {
    // 获取启用的服务类型（宠主端只显示启用的套餐）
    const res = await serviceTypeApi.getEnabled();
    if (res && res.code === 200 && res.data && Array.isArray(res.data)) {
      servicePackages.value = res.data.filter(pkg => pkg.status === 1); // 确保只显示启用的
      console.log("获取到服务套餐列表:", servicePackages.value);
      
      if (servicePackages.value.length === 0) {
        ElMessage.warning("当前没有可用的服务套餐，请联系管理员");
      }
      // 若推荐已到位，补一次默认选中
      applySmartSelection();
    } else {
      ElMessage.error("获取服务套餐失败");
      console.error("服务套餐API响应格式错误:", res);
    }
  } catch (error: any) {
    console.error("获取服务套餐失败:", error);
    ElMessage.error(error?.message || "获取服务套餐失败，请稍后重试");
  } finally {
    loading.value = false;
  }
};

// 页面加载时获取服务套餐列表
onMounted(() => {
  loadServicePackages();
  loadVisitorProfile();
  // 如果带 recommend=1 或者已保存访客信息，则拉取推荐
  if (route.query.recommend === "1") {
    loadRecommendation();
  } else if (sessionStorage.getItem(VISITOR_PROFILE_KEY)) {
    loadRecommendation();
  }
});
</script>

<style scoped lang="scss">
// 治愈系配色方案
$cream-beige: #FAF4E4; // 米色
$soft-green: #BBD446; // 柔和绿色
$warm-yellow: #FCD744; // 温暖黄色
$coral-orange: #FA7B53; // 珊瑚橙
$soft-pink: #FFCCCC; // 柔和粉色
$soft-blue: #CCFFFF; // 柔和蓝色
$soft-purple: #CCCCFF; // 柔和紫色
$soft-lime: #CCFF99; // 柔和青柠
$warm-white: #FFFFFF; // 白色
$text-dark: #3d2c23; // 深棕色文字（高对比度）
$text-medium: #6e5a4f; // 中等棕色文字
$text-light: #9a857a; // 浅棕色文字
$border-soft: #f1e3d8; // 柔和边框

.service-packages-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 20px;
  min-height: calc(100vh - 100px);
  background: radial-gradient(900px 500px at 50% 0%, rgba(250, 244, 228, 0.3) 0%, rgba(255, 255, 255, 0) 60%),
    linear-gradient(135deg, #fff7f2 0%, #faf4e4 50%, #fff 100%);
}

.packages-intro {
  text-align: center;
  margin-bottom: 50px;
  padding: 40px 0;

  h2 {
    color: $text-dark;
    margin-bottom: 15px;
    font-size: 36px;
    font-weight: 700;
    letter-spacing: 0.5px;
    position: relative;
    display: inline-block;
    
    &::after {
      content: "";
      display: block;
      width: 80px;
      height: 4px;
      background: linear-gradient(90deg, $coral-orange, $warm-yellow);
      margin: 15px auto 0;
      border-radius: 2px;
    }
  }

  p {
    color: $text-medium;
    font-size: 17px;
    line-height: 2;
    margin-top: 20px;
    font-weight: 400;
  }
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 30px;
  margin-bottom: 50px;
}

.smart-recommend-card {
  border-radius: 16px;
  margin: 0 0 30px;
  border: 1px solid $border-soft;
  box-shadow: 0 4px 20px rgba(61, 44, 35, 0.08);
  background: $warm-white;
  
  :deep(.el-card__header) {
    background: linear-gradient(135deg, rgba(187, 212, 70, 0.15) 0%, rgba(252, 215, 68, 0.12) 50%, rgba(250, 123, 83, 0.1) 100%);
    border-bottom: 2px solid $border-soft;
    padding: 20px 24px;
  }
  
  :deep(.el-card__body) {
    padding: 24px;
    background: $warm-white;
  }
}

.smart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  .left {
    display: flex;
    flex-direction: column;
    gap: 6px;
    .title {
      font-size: 18px;
      font-weight: 700;
      color: #303133;
    }
    .sub {
      font-size: 12px;
      color: #909399;
    }
  }
  .right {
    display: flex;
    gap: 10px;
  }
}

.smart-body {
  .smart-line {
    color: $text-medium;
    margin-bottom: 10px;
  }
  .smart-reco {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 16px;
    .name {
      font-weight: 700;
      color: $text-dark;
    }
    .score {
      margin-left: 8px;
    }
  }
  .smart-section {
    margin-top: 14px;
    .sec-title {
      font-weight: 600;
      color: $text-dark;
      margin-bottom: 6px;
    }
    .sec-list {
      margin: 0;
      padding-left: 18px;
      color: $text-medium;
      li {
        line-height: 1.8;
      }
    }
    .sec-text {
      color: $text-medium;
      line-height: 1.8;
    }
    .warn {
      margin-top: 10px;
    }
  }
  .smart-actions {
    margin-top: 14px;
    display: flex;
    justify-content: flex-end;
    gap: 10px;

    :deep(.el-button--primary) {
      background-color: $coral-orange;
      border-color: $coral-orange;
      color: #fff;

      &:hover {
        filter: brightness(1.05);
      }
    }
  }
}

.package-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
  position: relative;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 24px rgba(61, 44, 35, 0.12);
  }

  :deep(.el-card__header) {
    // 顶部改为柔和的米色纯色背景，避免刺眼渐变
    background-color: $cream-beige;
    color: $text-dark;
    padding: 20px 22px;
    border-bottom: none;
  }

  :deep(.el-card__body) {
    padding: 24px;
    background: $warm-white;

    // 卡片内部主按钮（选择套餐）使用柔和珊瑚橙，而不是默认蓝色
    .card-footer {
      :deep(.el-button--primary) {
        background-color: $coral-orange;
        border-color: $coral-orange;
        color: #fff;

        &.selected-btn {
          background-color: $soft-green;
          border-color: $soft-green;
        }

        &:hover {
          filter: brightness(1.05);
        }
      }
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
  }

  h3 {
    margin: 0;
    color: $warm-white;
    font-size: 22px;
    font-weight: 600;
  }

  .recommend-tag {
    margin-left: 8px;
  }

  .detail-btn {
    color: white;
    padding: 4px 12px;
    border-radius: 6px;
    transition: all 0.2s;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    .el-icon {
      margin-right: 4px;
    }
  }
}

.selected-package {
  border: 2px solid $soft-green;
  box-shadow: 0 0 18px rgba(187, 212, 70, 0.35);

  :deep(.el-card__header) {
    // 选中状态使用略带绿色调的米色，保持柔和
    background-color: lighten($soft-green, 32%);
  }
}

.package-content {
  text-align: center;
}

.package-price {
  margin: 20px 0 24px;
  padding: 15px;
  background-color: #fffaf5; // 柔和暖白，替代原来的渐变背景
  border-radius: 8px;

  .price {
    font-size: 42px;
    font-weight: bold;
    color: $coral-orange;
    display: block;
    margin-bottom: 5px;
  }

  .unit {
    font-size: 16px;
    color: $text-light;
  }
}

.package-description {
  color: $text-medium;
  margin-bottom: 20px;
  line-height: 1.8;
  text-align: left;
  min-height: 48px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.package-features-preview {
  margin: 20px 0;
  text-align: left;

  .features-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      padding: 10px 0;
      display: flex;
      align-items: flex-start;
      color: $text-medium;
      font-size: 14px;
      line-height: 1.6;

      .el-icon {
        margin-right: 10px;
        color: $soft-green;
        margin-top: 2px;
        flex-shrink: 0;
      }

      span {
        flex: 1;
      }

      &.more-features {
        color: $text-light;
        font-style: italic;

        .el-icon {
          color: $text-light;
        }
      }
    }
  }
}

.card-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid $border-soft;

  :deep(.el-button--primary) {
    background-color: $coral-orange;
    border-color: $coral-orange;
    color: #fff;

    &:hover {
      filter: brightness(1.05);
    }
  }

  .selected-btn {
    background: $soft-green;
    border-color: $soft-green;
    color: #fff;
  }
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  padding: 30px 0;
  background: rgba(250, 244, 228, 0.96);
  border-radius: 12px;
  position: sticky;
  bottom: 20px;
  box-shadow: 0 -4px 18px rgba(61, 44, 35, 0.12);

  :deep(.el-button) {
    min-width: 120px;
    border-radius: 999px;
    font-weight: 500;
  }

  :deep(.el-button--primary) {
    background-color: $coral-orange;
    border-color: $coral-orange;
    color: #fff;

    &:hover {
      filter: brightness(1.05);
    }
  }
}

// 详情对话框样式
:deep(.package-detail-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, rgba(250, 123, 83, 0.95) 0%, rgba(252, 215, 68, 0.95) 55%, rgba(187, 212, 70, 0.95) 100%);
    color: $warm-white;
    padding: 20px 24px;
    border-radius: 8px 8px 0 0;

    .el-dialog__title {
      color: $warm-white;
      font-size: 24px;
      font-weight: 600;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: $warm-white;
      font-size: 20px;
    }
  }

  .el-dialog__body {
    padding: 30px;
  }
}

.package-detail-content {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    padding-bottom: 20px;
    border-bottom: 2px solid $border-soft;

    .detail-price {
      .price-large {
        font-size: 48px;
        font-weight: bold;
        color: $coral-orange;
        margin-right: 10px;
      }

      .unit-large {
        font-size: 18px;
        color: $text-light;
      }
    }
  }

  .detail-section {
    margin-bottom: 30px;

    h4 {
      color: $text-dark;
      font-size: 18px;
      font-weight: 600;
      margin-bottom: 15px;
      padding-bottom: 10px;
      border-bottom: 2px solid $coral-orange;
      display: inline-block;
    }

    p {
      color: $text-medium;
      line-height: 1.8;
      font-size: 15px;
    }
  }

  .detail-features {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      padding: 12px 0;
      display: flex;
      align-items: flex-start;
      color: $text-medium;
      font-size: 15px;
      line-height: 1.8;
      border-bottom: 1px solid #f5f7fa;

      &:last-child {
        border-bottom: none;
      }

      .el-icon {
        margin-right: 12px;
        color: $soft-green;
        margin-top: 3px;
        flex-shrink: 0;
        font-size: 18px;
      }

      span {
        flex: 1;
      }
    }
  }

  .process-content {
    background: $cream-beige;
    padding: 20px;
    border-radius: 8px;
    color: $text-medium;
    line-height: 2;
    white-space: pre-wrap;
    font-size: 15px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .packages-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .packages-intro {
    h2 {
      font-size: 24px;
    }
  }
}
</style>
