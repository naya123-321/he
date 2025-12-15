<template>
  <div class="service-packages-container">
    <el-page-header @back="goBack" content="选择服务套餐" />

    <div class="packages-intro">
      <h2>专业的宠物殡葬服务</h2>
      <p>我们提供多种服务套餐，根据您的需求和预算选择最适合的方案</p>
    </div>

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
                :type="isRecommended(pkg) ? 'success' : 'info'"
                size="small"
                class="recommend-tag"
              >
                {{ isRecommended(pkg) ? "推荐" : "标准" }}
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
        下一步，预约服务
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
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElEmpty, ElDialog } from "element-plus";
import { Check, View, MoreFilled } from "@element-plus/icons-vue";
import { serviceTypeApi, type ServiceTypeVO } from "@/api/order";

const router = useRouter();
const selectedPackageId = ref<number | null>(null);
const selectedPackage = ref<ServiceTypeVO | null>(null);
const loading = ref(false);
const detailDialogVisible = ref(false);
const currentPackage = ref<ServiceTypeVO | null>(null);

// 服务套餐数据（从API获取）
const servicePackages = ref<ServiceTypeVO[]>([]);

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

// 判断是否推荐（使用数据库字段）
const isRecommended = (pkg: ServiceTypeVO): boolean => {
  // 使用数据库中的 isRecommended 字段
  return pkg.isRecommended === true;
};

// 返回上一页
const goBack = () => {
  router.back();
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

  // 将选中的套餐信息传递给预约页面
  router.push({
    path: "/pet-owner/book-service",
    query: {
      packageId: String(selectedPackageId.value),
      packageName: selectedPackage.value.name,
    },
  });
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
});
</script>

<style scoped lang="scss">
.service-packages-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 100px);
}

.packages-intro {
  text-align: center;
  margin-bottom: 50px;
  padding: 30px 0;

  h2 {
    color: #303133;
    margin-bottom: 15px;
    font-size: 32px;
    font-weight: 600;
  }

  p {
    color: #606266;
    font-size: 16px;
    line-height: 1.8;
  }
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 30px;
  margin-bottom: 50px;
}

.package-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
  position: relative;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  :deep(.el-card__header) {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 20px;
    border-bottom: none;
  }

  :deep(.el-card__body) {
    padding: 24px;
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
    color: white;
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
  border: 3px solid #409eff;
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.4);

  :deep(.el-card__header) {
    background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  }
}

.package-content {
  text-align: center;
}

.package-price {
  margin: 20px 0 24px;
  padding: 15px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 8px;

  .price {
    font-size: 42px;
    font-weight: bold;
    color: #e6a23c;
    display: block;
    margin-bottom: 5px;
  }

  .unit {
    font-size: 16px;
    color: #909399;
  }
}

.package-description {
  color: #606266;
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
      color: #606266;
      font-size: 14px;
      line-height: 1.6;

      .el-icon {
        margin-right: 10px;
        color: #67c23a;
        margin-top: 2px;
        flex-shrink: 0;
      }

      span {
        flex: 1;
      }

      &.more-features {
        color: #909399;
        font-style: italic;

        .el-icon {
          color: #909399;
        }
      }
    }
  }
}

.card-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;

  .selected-btn {
    background: #67c23a;
    border-color: #67c23a;
  }
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  padding: 30px 0;
  background: #f5f7fa;
  border-radius: 12px;
  position: sticky;
  bottom: 20px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

// 详情对话框样式
:deep(.package-detail-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 20px 24px;
    border-radius: 8px 8px 0 0;

    .el-dialog__title {
      color: white;
      font-size: 24px;
      font-weight: 600;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: white;
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
    border-bottom: 2px solid #ebeef5;

    .detail-price {
      .price-large {
        font-size: 48px;
        font-weight: bold;
        color: #e6a23c;
        margin-right: 10px;
      }

      .unit-large {
        font-size: 18px;
        color: #909399;
      }
    }
  }

  .detail-section {
    margin-bottom: 30px;

    h4 {
      color: #303133;
      font-size: 18px;
      font-weight: 600;
      margin-bottom: 15px;
      padding-bottom: 10px;
      border-bottom: 2px solid #409eff;
      display: inline-block;
    }

    p {
      color: #606266;
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
      color: #606266;
      font-size: 15px;
      line-height: 1.8;
      border-bottom: 1px solid #f5f7fa;

      &:last-child {
        border-bottom: none;
      }

      .el-icon {
        margin-right: 12px;
        color: #67c23a;
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
    background: #f5f7fa;
    padding: 20px;
    border-radius: 8px;
    color: #606266;
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
