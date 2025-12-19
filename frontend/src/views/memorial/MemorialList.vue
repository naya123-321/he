<template>
  <div class="memorial-list">
    <div class="page-header">
      <div>
        <h2 class="title">我的纪念册</h2>
        <div class="subtitle">记录每一个美好的生命瞬间</div>
      </div>
      <div class="actions">
        <el-button type="primary" @click="goCreate">新建纪念册</el-button>
      </div>
    </div>

    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <div class="status">
          <el-segmented
            v-model="status"
            :options="statusOptions"
            size="default"
          />
          </div>
        <div class="search">
              <el-input
            v-model="keyword"
                clearable
            placeholder="搜索纪念册标题"
            @keyup.enter="reload"
            @clear="reload"
              >
            <template #append>
              <el-button @click="reload">搜索</el-button>
                </template>
              </el-input>
          </div>
        </div>
      </el-card>

    <el-card shadow="never" class="content">
      <div v-loading="loading" class="list-wrap">
        <el-empty
          v-if="!loading && memorials.length === 0"
          description="暂无纪念册"
        />

        <el-row v-else :gutter="16">
          <el-col
            v-for="m in memorials"
            :key="m.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
          >
            <el-card shadow="hover" class="card">
              <div class="card-title" @click="goPreview(m)">
                {{ m.title }}
              </div>
              <div class="card-meta">
                <span class="meta-item">宠物：{{ m.petName }}</span>
                <span v-if="m.petType" class="meta-item">({{ getPetTypeLabel(m.petType) || m.petType }})</span>
              </div>
              <div class="card-meta">
                <span class="meta-item">状态：{{ m.statusText || statusText(m.status) }}</span>
                </div>
              <div class="card-meta">
                <span class="meta-item">设计协作：{{ m.designStatusText || "-" }}</span>
              </div>
              <div class="card-meta">
                <span class="meta-item">关联订单：{{ m.orderId ? `#${m.orderId}` : "未关联" }}</span>
              </div>
              <div class="card-actions">
                <el-button
                  v-if="m.designStatus === 0"
                  type="warning"
                  size="small"
                  @click="submitDesign(m)"
                >
                  提交设计
                </el-button>
                <el-button
                  v-if="m.designStatus === 0 && !m.orderId"
                  type="info"
                  size="small"
                  @click="openBindOrder(m)"
                >
                  关联订单
                </el-button>
                <el-button
                  v-if="m.designStatus === 20 || m.designStatus === 30"
                  type="success"
                  size="small"
                  @click="openDesignPreview(m)"
                >
                  查看设计稿
                </el-button>
                <el-button v-else type="success" size="small" @click="goPreview(m)">
                  查看
                </el-button>
                <el-button
                  v-if="m.designStatus !== 60"
                  type="danger"
                  plain
                  size="small"
                  @click="remove(m)"
                >
                  删除
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          :current-page="pagination.current"
          :page-size="pagination.pageSize"
          :page-sizes="[8, 12, 16, 24]"
          @update:current-page="handlePageChange"
          @update:page-size="handleSizeChange"
        />
                  </div>
    </el-card>

    <!-- 关联订单弹窗 -->
    <el-dialog v-model="bindDialogVisible" title="关联订单" width="620px" @close="resetBindDialog">
      <el-form label-width="110px">
        <el-form-item label="选择订单">
          <el-select v-model="selectedOrderId" filterable clearable style="width: 100%" :loading="loadingOrders">
            <el-option
              v-for="o in orders"
              :key="o.id"
              :label="`${o.petName || '未命名宠物'}｜${o.statusText || `状态${o.status}`}${o.orderNo ? `｜${o.orderNo}` : ''}`"
              :value="o.id"
            />
          </el-select>
          <div class="tip">关联订单后才能提交设计；订单必须是你自己的订单。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="bindOrder">确定关联</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { memorialApi, type MemorialVO } from "@/api/memorial";
import { useMemorialStore } from "@/store/memorial";
import { orderApi, type OrderVO } from "@/api/order";
import { getPetTypeLabel } from "@/constants/petTypes";

const router = useRouter();
const memorialStore = useMemorialStore();

const loading = computed(() => memorialStore.loading);
const memorials = computed(() => memorialStore.memorials);
const pagination = computed(() => memorialStore.pagination);

const status = ref<"all" | "0" | "1" | "2" | "3">("all");
const keyword = ref("");

// 关联订单
const bindDialogVisible = ref(false);
const binding = ref(false);
const bindingMemorialId = ref<number | null>(null);
const orders = ref<OrderVO[]>([]);
const loadingOrders = ref(false);
const selectedOrderId = ref<number | null>(null);

async function loadMyOrders() {
  loadingOrders.value = true;
  try {
    const res = await orderApi.getOrderList({ pageNum: 1, pageSize: 50 });
    orders.value = res.data?.records || [];
  } catch {
    orders.value = [];
  } finally {
    loadingOrders.value = false;
  }
}

function openBindOrder(m: MemorialVO) {
  bindingMemorialId.value = m.id;
  selectedOrderId.value = (m.orderId as any) ?? null;
  bindDialogVisible.value = true;
}

function resetBindDialog() {
  bindingMemorialId.value = null;
  selectedOrderId.value = null;
}

async function bindOrder() {
  if (!bindingMemorialId.value) return;
  if (!selectedOrderId.value) {
    ElMessage.warning("请选择订单");
    return;
  }
  binding.value = true;
  try {
    await memorialApi.bindOrder(bindingMemorialId.value, selectedOrderId.value);
    ElMessage.success("关联成功");
    bindDialogVisible.value = false;
    await reload();
  } catch (e: any) {
    ElMessage.error(e?.message || "关联失败");
  } finally {
    binding.value = false;
  }
}

const statusOptions = [
  { label: "全部", value: "all" },
  { label: "草稿", value: "0" },
  { label: "待审核", value: "1" },
  { label: "已完成", value: "2" },
  { label: "已下架", value: "3" },
];

function statusText(s: number) {
  switch (s) {
    case 0:
      return "草稿";
    case 1:
      return "待审核";
    case 2:
      return "已完成";
    case 3:
      return "已下架";
    default:
      return "未知";
  }
}

async function reload() {
  const statusNum =
    status.value === "all" ? undefined : Number.parseInt(status.value, 10);

  await memorialStore.fetchMemorials({
    status: Number.isNaN(statusNum as number) ? undefined : statusNum,
    title: keyword.value ? keyword.value : undefined,
  });
}

watch(
  () => status.value,
  async () => {
    // 切换状态时应立即刷新列表，并回到第一页
    memorialStore.pagination.current = 1;
    await reload();
  }
);

function goCreate() {
  router.push("/memorial/create");
}

function goPreview(m: MemorialVO) {
  router.push(`/memorial/preview/${m.id}`);
}

async function submitDesign(m: MemorialVO) {
  try {
    await ElMessageBox.confirm(
      "确定提交到服务端设计吗？提交后服务人员将开始制作设计稿。",
      "提交确认",
      {
        type: "warning",
        confirmButtonText: "确定提交",
        cancelButtonText: "取消",
      }
    );
    await memorialApi.submitForDesign(m.id);
    ElMessage.success("已提交到服务端设计");
    await reload();
  } catch (e: any) {
    if (e?.message) ElMessage.error(e.message);
  }
}

function openDesignPreview(m: MemorialVO) {
  router.push(`/memorial/preview/${m.id}`);
}

async function remove(m: MemorialVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除纪念册“${m.title}”吗？此操作不可恢复。`,
      "删除确认",
      {
        confirmButtonText: "确定删除",
        cancelButtonText: "取消",
        type: "warning",
      }
    );

    const res = await memorialApi.deleteMemorial(m.id);
    if (res?.code === 200) {
      ElMessage.success("删除成功");
      await reload();
    } else {
      ElMessage.error(res?.message || "删除失败");
    }
  } catch (e) {
    // ignore cancel
  }
}

function handlePageChange(page: number) {
  memorialStore.pagination.current = page;
  reload();
}

function handleSizeChange(size: number) {
  memorialStore.pagination.pageSize = size;
  memorialStore.pagination.current = 1;
  reload();
}

onMounted(() => {
  reload();
  loadMyOrders();
});
</script>

<style scoped lang="scss">
// 统一配色方案（与其他页面保持一致）
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$text-primary: #303133;
$text-secondary: #606266;
$text-light: #909399;
$border-color: #ebeef5;
$bg-light: #f5f7fa;
$bg-white: #ffffff;

.memorial-list {
  padding: 30px 20px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 100px);
  background: #ffffff;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 2px solid $border-color;

  .title {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: 0.5px;
  }

  .subtitle {
    margin-top: 8px;
    color: $text-secondary;
    font-size: 14px;
    font-weight: 400;
  }

  .actions {
    :deep(.el-button--primary) {
      background-color: $primary-color;
      border-color: $primary-color;
      border-radius: 8px;
      padding: 10px 24px;
      font-weight: 500;

      &:hover {
        filter: brightness(1.05);
      }
    }
  }
}

.toolbar {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid $border-color;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  :deep(.el-card__body) {
    padding: 20px;
  }

  .toolbar-row {
    display: flex;
    gap: 16px;
    align-items: center;
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .status {
    :deep(.el-segmented) {
      background-color: $bg-light;
      border-radius: 8px;
    }
  }

  .search {
    width: min(520px, 100%);

    :deep(.el-input__wrapper) {
      border-radius: 8px;
    }

    :deep(.el-button) {
      border-radius: 0 8px 8px 0;
    }
  }
}

.content {
  border-radius: 12px;
  border: 1px solid $border-color;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  :deep(.el-card__body) {
    padding: 24px;
  }

  .list-wrap {
    min-height: 300px;
  }
}

.card {
  margin-bottom: 0;
  border-radius: 12px;
  border: 1px solid $border-color;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: default;
  height: 100%;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    transform: translateY(-4px);
    border-color: $primary-color;
  }

  :deep(.el-card__body) {
    padding: 20px;
  }

  .card-title {
    font-weight: 600;
    font-size: 18px;
    margin-bottom: 12px;
    cursor: pointer;
    color: $text-primary;
    transition: color 0.3s;
    line-height: 1.4;

    &:hover {
      color: $primary-color;
    }
  }

  .card-meta {
    color: $text-secondary;
    font-size: 13px;
    margin-bottom: 10px;
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    line-height: 1.6;

    .meta-item {
      padding: 4px 8px;
      background: $bg-light;
      border-radius: 4px;
    }
  }

  .card-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid $border-color;
    flex-wrap: wrap;

    :deep(.el-button) {
      border-radius: 6px;
      font-weight: 500;

      &.el-button--warning {
        background-color: $warning-color;
        border-color: $warning-color;
      }

      &.el-button--success {
        background-color: $success-color;
        border-color: $success-color;
      }

      &.el-button--danger {
        color: $danger-color;
        border-color: $danger-color;

        &:hover {
          background-color: rgba(245, 108, 108, 0.1);
        }
      }

      &.el-button--info {
        background-color: $text-secondary;
        border-color: $text-secondary;
      }
    }
  }
}

.pager {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid $border-color;

  :deep(.el-pagination) {
    .el-pagination__total,
    .el-pagination__jump {
      color: $text-secondary;
    }

    .btn-prev,
    .btn-next,
    .el-pager li {
      border-radius: 6px;
    }

    .el-pager li.is-active {
      background-color: $primary-color;
      color: $bg-white;
    }
  }
}

// 优化对话框样式
:deep(.el-dialog) {
  border-radius: 12px;

  .el-dialog__header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 20px 24px;
    border-radius: 12px 12px 0 0;

    .el-dialog__title {
      color: $bg-white;
      font-size: 18px;
      font-weight: 600;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: $bg-white;
      font-size: 20px;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-form-item__label {
    color: $text-secondary;
    font-weight: 500;
  }

  .el-select,
  .el-input__wrapper {
    border-radius: 8px;
  }

  .el-button--primary {
    background-color: $primary-color;
    border-color: $primary-color;
    border-radius: 8px;
  }
}

.tip {
  margin-top: 8px;
  color: $text-light;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px 12px;
  background: $bg-light;
  border-radius: 6px;
  border-left: 3px solid $primary-color;
}

// 优化空状态
:deep(.el-empty) {
  padding: 60px 0;
}
</style>



