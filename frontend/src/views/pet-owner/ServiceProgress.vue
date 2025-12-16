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

      <!-- 骨灰寄存模块 -->
      <el-card shadow="never" style="margin-top: 16px">
        <template #header>
          <div class="section-header">
            <span class="section-title">骨灰寄存</span>
            <div class="section-actions">
              <el-button v-if="currentOrder.status === 3 && !urnRecord" type="primary" @click="openUrnApply">
                申请寄存
              </el-button>
              <el-button @click="goUrnStorage">去骨灰寄存管理</el-button>
            </div>
          </div>
        </template>

        <el-skeleton v-if="urnLoading" :rows="3" animated />

        <el-empty
          v-else-if="!urnRecord"
          description="暂无寄存记录"
          :image-size="120"
        >
          <div class="form-tip" style="margin-top: 8px;">
            订单完成后可申请骨灰寄存，管理员审核通过后进入寄存中。
          </div>
        </el-empty>

        <div v-else class="urn-summary">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="寄存状态">
              <el-tag :type="urnStatusType(urnRecord.status)">{{ urnRecord.statusText }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="骨灰盒编号">{{ urnRecord.urnNo }}</el-descriptions-item>
            <el-descriptions-item label="寄存日期">{{ urnRecord.storageDate }}</el-descriptions-item>
            <el-descriptions-item label="到期日期">{{ urnRecord.expiryDate }}</el-descriptions-item>
            <el-descriptions-item label="寄存位置" :span="2">
              {{ urnRecord.storageLocation || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              {{ urnRecord.remark || "—" }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <!-- 满意度评价模块（仅已完成订单） -->
      <el-card v-if="currentOrder.status === 3" shadow="never" style="margin-top: 16px">
        <template #header>
          <div class="section-header">
            <span class="section-title">满意度评价</span>
          </div>
        </template>

        <div v-if="currentOrder.rating" class="review-readonly">
          <div class="row">
            <span class="label">评分：</span>
            <el-rate :model-value="currentOrder.rating" disabled />
            <span class="value" style="margin-left: 8px;">{{ currentOrder.rating }} 星</span>
          </div>
          <div class="row" v-if="currentOrder.review">
            <span class="label">评价：</span>
            <span class="value">{{ currentOrder.review }}</span>
          </div>
          <div class="form-tip">感谢你的评价，我们会持续改进服务。</div>
        </div>

        <div v-else class="review-form">
          <div class="row">
            <span class="label">评分：</span>
            <el-rate v-model="reviewRating" />
            <span class="value" style="margin-left: 8px;">{{ reviewRating }} 星</span>
          </div>
          <el-input
            v-model="reviewText"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="写下你的评价（选填）"
            style="margin: 10px 0"
          />
          <el-button type="primary" :loading="reviewSubmitting" @click="submitOrderReview">
            提交评价
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- 申请骨灰寄存弹窗 -->
    <el-dialog v-model="urnDialogVisible" title="申请骨灰寄存" width="640px" @close="resetUrnForm">
      <el-form ref="urnFormRef" :model="urnForm" :rules="urnRules" label-width="120px">
        <el-form-item label="骨灰盒编号" prop="urnNo">
          <el-input v-model="urnForm.urnNo" placeholder="请输入骨灰盒编号" />
        </el-form-item>
        <el-form-item label="寄存日期" prop="storageDate">
          <el-date-picker v-model="urnForm.storageDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="寄存期限(月)" prop="storagePeriod">
          <el-input-number v-model="urnForm.storagePeriod" :min="1" :max="60" style="width: 100%" />
        </el-form-item>
        <el-form-item label="寄存位置">
          <el-input v-model="urnForm.storageLocation" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="urnForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="urnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="urnSubmitting" @click="submitUrnApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { orderApi, type OrderVO } from "@/api/order";
import { urnStorageApi, type UrnStorageCreateDTO, type UrnStorageVO } from "@/api/urnStorage";

const router = useRouter();

const ordersLoading = ref(false);
const orders = ref<OrderVO[]>([]);
const selectedOrderId = ref<number | null>(null);
const currentOrder = ref<OrderVO | null>(null);

// 骨灰寄存
const urnLoading = ref(false);
const urnRecord = ref<UrnStorageVO | null>(null);
const urnDialogVisible = ref(false);
const urnSubmitting = ref(false);
const urnFormRef = ref<any>(null);
const urnForm = reactive<UrnStorageCreateDTO>({
  orderId: 0,
  petName: "",
  petType: "",
  urnNo: "",
  storageDate: "",
  storagePeriod: 12,
  storageLocation: "",
  remark: "",
});
const urnRules = {
  urnNo: [{ required: true, message: "请输入骨灰盒编号", trigger: "blur" }],
  storageDate: [{ required: true, message: "请选择寄存日期", trigger: "change" }],
  storagePeriod: [{ required: true, message: "请输入寄存期限", trigger: "change" }],
};

function urnStatusType(status: number) {
  if (status === -1) return "warning";
  if (status === 0) return "success";
  if (status === 1) return "info";
  if (status === 2) return "danger";
  return "info";
}

async function loadUrnStorage(orderId: number) {
  urnLoading.value = true;
  try {
    const res = await urnStorageApi.getUrnStorageList({ orderId, pageNum: 1, pageSize: 1 });
    urnRecord.value = res.data?.records?.[0] || null;
  } catch {
    urnRecord.value = null;
  } finally {
    urnLoading.value = false;
  }
}

function goUrnStorage() {
  router.push({ path: "/pet-owner/urn-storage", query: { orderId: selectedOrderId.value || undefined } });
}

function openUrnApply() {
  if (!currentOrder.value) return;
  urnForm.orderId = currentOrder.value.id;
  urnForm.petName = currentOrder.value.petName;
  urnForm.petType = currentOrder.value.petType || "";
  urnForm.urnNo = "";
  urnForm.storageDate = "";
  urnForm.storagePeriod = 12;
  urnForm.storageLocation = "";
  urnForm.remark = "";
  urnDialogVisible.value = true;
}

function resetUrnForm() {
  urnFormRef.value?.clearValidate?.();
}

async function submitUrnApply() {
  if (!urnFormRef.value) return;
  try {
    await urnFormRef.value.validate();
  } catch {
    return;
  }
  urnSubmitting.value = true;
  try {
    await urnStorageApi.createUrnStorage(urnForm);
    ElMessage.success("已提交寄存申请，等待管理员审批");
    urnDialogVisible.value = false;
    if (currentOrder.value?.id) await loadUrnStorage(currentOrder.value.id);
  } catch (e: any) {
    ElMessage.error(e?.message || "提交失败");
  } finally {
    urnSubmitting.value = false;
  }
}

// 满意度评价
const reviewRating = ref(5);
const reviewText = ref("");
const reviewSubmitting = ref(false);

async function submitOrderReview() {
  if (!currentOrder.value) return;
  if (reviewRating.value < 1 || reviewRating.value > 5) {
    ElMessage.warning("请选择 1-5 星评分");
    return;
  }
  reviewSubmitting.value = true;
  try {
    await orderApi.submitReview(currentOrder.value.id, reviewRating.value, reviewText.value);
    ElMessage.success("评价提交成功");
    await loadOrderDetail(currentOrder.value.id);
  } catch (e: any) {
    ElMessage.error(e?.message || "评价提交失败");
  } finally {
    reviewSubmitting.value = false;
  }
}

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
    await loadUrnStorage(id);
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

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.section-title {
  font-weight: 700;
}

.section-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.form-tip {
  color: #909399;
  font-size: 12px;
}

.review-form,
.review-readonly {
  .row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    flex-wrap: wrap;
  }
  .label {
    color: #606266;
    min-width: 48px;
  }
  .value {
    color: #303133;
  }
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



