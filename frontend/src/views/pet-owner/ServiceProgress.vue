<template>
  <div class="service-progress-container">
    <el-page-header @back="goBack" content="服务进度" />

    <div class="progress-intro">
      <h2>您的服务进度</h2>
      <p>实时查看您的宠物殡葬服务进度和服务人员信息</p>
    </div>

    <el-card shadow="never" class="order-selector-card">
      <el-form inline>
        <el-form-item label="选择订单">
          <el-select
            v-model="selectedOrderId"
            placeholder="请选择订单"
            style="width: 360px"
            filterable
            clearable
            @change="handleOrderChange"
          >
            <el-option
              v-for="o in orders"
              :key="o.id"
              :label="`${o.orderNo} - ${o.petName}（${o.statusText}）`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="reloadOrders" :loading="ordersLoading">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty
      v-if="!ordersLoading && orders.length === 0"
      description="暂无订单，先去预约服务吧"
      :image-size="140"
      style="margin-top: 24px"
    >
      <el-button type="primary" @click="goBookService">去预约服务</el-button>
    </el-empty>

    <template v-if="currentOrder">
      <el-alert
        v-if="currentOrder.status === 4"
        type="error"
        :closable="false"
        show-icon
        style="margin: 16px 0"
        title="该订单已取消"
      />

      <div class="status-cards">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8">
            <el-card shadow="hover" class="status-card status-card-primary">
              <div class="status-content">
                <div class="status-icon-wrapper">
                  <el-icon class="status-icon"><Flag /></el-icon>
                </div>
                <div class="status-info">
                  <div class="status-label">当前状态</div>
                  <div class="status-value">{{ currentOrder.statusText }}</div>
                  <div class="status-desc">订单号：{{ currentOrder.orderNo }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8">
            <el-card shadow="hover" class="status-card status-card-time">
              <div class="status-content">
                <div class="status-icon-wrapper">
                  <el-icon class="status-icon"><Clock /></el-icon>
                </div>
                <div class="status-info">
                  <div class="status-label">预约时间</div>
                  <div class="status-value">{{ currentOrder.appointmentTime || "未设置" }}</div>
                  <div class="status-desc">下单时间：{{ currentOrder.createTime }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8">
            <el-card
              shadow="hover"
              class="status-card status-card-personnel"
              :class="{ 'no-assigned': !currentOrder.serviceProviderId }"
            >
              <div class="status-content">
                <div class="status-icon-wrapper">
                  <el-icon class="status-icon"><User /></el-icon>
                </div>
                <div class="status-info">
                  <div class="status-label">服务人员</div>
                  <div class="status-value personnel-name">
                    {{ currentOrder.serviceProviderName || "未分配" }}
                  </div>
                  <div class="status-desc personnel-phone">
                    {{ currentOrder.serviceProviderPhone || "—" }}
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header>
          <div style="font-weight: 600">进度概览</div>
        </template>

        <el-steps :active="activeStep" align-center finish-status="success">
          <el-step title="待接单" />
          <el-step title="已接单" />
          <el-step title="服务中" />
          <el-step title="已完成" />
        </el-steps>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div style="font-weight: 600">订单信息</div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="服务套餐">
            {{ currentOrder.serviceTypeName }}（￥{{ currentOrder.servicePrice }}）
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            {{ currentOrder.paymentStatusText }}
          </el-descriptions-item>
          <el-descriptions-item label="宠物信息">
            {{ currentOrder.petName }} / {{ currentOrder.petType }}
          </el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ currentOrder.contactName }}（{{ currentOrder.contactPhone }}）
          </el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">
            {{ currentOrder.address }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.specialRequirements" label="特殊要求" :span="2">
            {{ currentOrder.specialRequirements }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.serviceNotes" label="服务记录" :span="2">
            {{ currentOrder.serviceNotes }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { orderApi, type OrderVO } from "@/api/order";

const router = useRouter();

const ordersLoading = ref(false);
const orders = ref<OrderVO[]>([]);
const selectedOrderId = ref<number | null>(null);
const currentOrder = ref<OrderVO | null>(null);

const activeStep = computed(() => {
  const s = currentOrder.value?.status;
  if (s == null) return 0;
  if (s <= 0) return 0;
  if (s === 1) return 1;
  if (s === 2) return 2;
  if (s === 3) return 3;
  return 0;
});

function goBack() {
  router.back();
}

function goBookService() {
  router.push("/pet-owner/service-packages");
}

async function reloadOrders() {
  ordersLoading.value = true;
  try {
    const res = await orderApi.getOrderList({ pageNum: 1, pageSize: 50 });
    orders.value = res.data?.records || [];
    // 默认选中最新一单
    if (!selectedOrderId.value && orders.value.length > 0) {
      selectedOrderId.value = orders.value[0].id;
      await loadOrderDetail(orders.value[0].id);
    }
  } catch (e: any) {
    orders.value = [];
    ElMessage.error(e?.message || "加载订单失败");
  } finally {
    ordersLoading.value = false;
  }
}

async function loadOrderDetail(id: number) {
  try {
    const res = await orderApi.getOrderDetail(id);
    currentOrder.value = res.data;
  } catch (e: any) {
    currentOrder.value = null;
    ElMessage.error(e?.message || "加载订单详情失败");
  }
}

async function handleOrderChange(id: number | null) {
  if (!id) {
    currentOrder.value = null;
    return;
  }
  await loadOrderDetail(id);
}

onMounted(() => {
  reloadOrders();
});
</script>

<style scoped lang="scss">
.service-progress-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.progress-intro {
  margin: 12px 0 20px;

  h2 {
    color: #303133;
    margin: 0 0 8px;
  }

  p {
    color: #606266;
    font-size: 14px;
    margin: 0;
  }
}

.order-selector-card {
  margin-bottom: 16px;
}

.status-cards {
  margin-bottom: 16px;
}

.status-card {
  border-radius: 14px;
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 18px;
  }

  .status-content {
    display: flex;
    gap: 14px;
    align-items: flex-start;
  }

  .status-icon-wrapper {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.04);
  }

  .status-icon {
    font-size: 28px;
  }

  .status-label {
    color: #606266;
    font-size: 12px;
    margin-bottom: 6px;
  }

  .status-value {
    font-size: 18px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 6px;
  }

  .status-desc {
    color: #909399;
    font-size: 12px;
  }
}

.status-card-primary {
  background: linear-gradient(135deg, #fff9e6 0%, #fff4d6 100%);
}

.status-card-time {
  background: linear-gradient(135deg, #fef2f8 0%, #fce7f3 100%);
}

.status-card-personnel {
  background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 100%);

  &.no-assigned {
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  }
}
</style>



