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
                <span v-if="m.petType" class="meta-item">({{ m.petType }})</span>
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
  .memorial-list {
  padding: 16px;
}

.page-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
  margin-bottom: 12px;

  .title {
        margin: 0;
        font-size: 20px;
    font-weight: 700;
  }

  .subtitle {
    margin-top: 4px;
              color: #909399;
              font-size: 12px;
  }
}

.toolbar {
  margin-bottom: 12px;

  .toolbar-row {
    display: flex;
    gap: 12px;
    align-items: center;
      flex-wrap: wrap;
    justify-content: space-between;
  }

  .search {
    width: min(520px, 100%);
  }
}

.content {
  .list-wrap {
    min-height: 240px;
  }
}

.card {
  margin-bottom: 12px;
  cursor: default;

          .card-title {
    font-weight: 700;
    margin-bottom: 6px;
            cursor: pointer;
          }

          .card-meta {
            color: #606266;
    font-size: 12px;
    margin-bottom: 6px;
              display: flex;
    gap: 6px;
    flex-wrap: wrap;
          }

          .card-actions {
            display: flex;
    gap: 8px;
    justify-content: flex-end;
    margin-top: 8px;
  }
}

.pager {
  margin-top: 12px;
      display: flex;
      justify-content: flex-end;
}

.tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
</style>



