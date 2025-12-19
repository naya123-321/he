<template>
  <div class="urn-storage-container">
    <el-page-header @back="goBack" content="骨灰寄存" />

    <div class="content-wrapper">
      <el-card shadow="never" class="action-card">
        <div class="action-bar">
          <el-button type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon>
            申请寄存
          </el-button>
          <el-button @click="loadList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
              <el-option label="待审批" :value="-1" />
              <el-option label="寄存中" :value="0" />
              <el-option label="已取回" :value="1" />
              <el-option label="已到期" :value="2" />
              <el-option label="已拒绝" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="宠物名">
            <el-input v-model="query.petName" placeholder="请输入宠物名" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="骨灰盒编号">
            <el-input v-model="query.urnNo" placeholder="请输入编号" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="resetToFirstPageAndLoad">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="hover">
        <el-table :data="list" v-loading="loading" border style="width: 100%">
          <el-table-column prop="orderNo" label="订单号" width="200" show-overflow-tooltip />
          <el-table-column prop="petName" label="宠物名" width="140" />
          <el-table-column prop="urnNo" label="骨灰盒编号" width="160" />
          <el-table-column prop="storageDate" label="寄存日期" width="120" />
          <el-table-column prop="expiryDate" label="到期日期" width="120" />
          <el-table-column prop="storagePeriod" label="期限(月)" width="100" />
          <el-table-column prop="statusText" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ row.statusText || statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openDetail(row.id)">详情</el-button>
              <el-button
                v-if="row.status === -1"
                type="primary"
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="canRetrieve(row.status)"
                type="warning"
                size="small"
                @click="retrieve(row.id)"
              >
                取回
              </el-button>
              <el-button
                v-if="row.status === -1"
                type="danger"
                size="small"
                @click="remove(row.id)"
              >
                撤销申请
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!loading && list.length === 0"
          description="暂无寄存记录"
          :image-size="140"
          style="padding: 24px 0"
        >
          <el-button type="primary" @click="openCreate">申请寄存</el-button>
        </el-empty>

        <div class="pager">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 创建/申请寄存 -->
    <el-dialog v-model="createDialogVisible" :title="createDialogTitle" width="760px" @close="handleCreateClose">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
        <el-form-item label="选择订单" prop="orderId">
          <el-select
            v-model="createForm.orderId"
            placeholder="请选择订单"
            style="width: 100%"
            filterable
            @change="handleOrderSelected"
            :disabled="isEditing"
          >
            <el-option
              v-for="o in myOrders"
              :key="o.id"
              :label="`${o.orderNo} - ${o.petName}（${o.statusText}）`"
              :value="o.id"
            />
          </el-select>
          <div class="form-tip">提示：仅用于绑定你的订单，后端会校验该订单必须属于你。</div>
        </el-form-item>

        <el-form-item label="宠物名" prop="petName">
          <el-input v-model="createForm.petName" maxlength="50" />
        </el-form-item>
        <el-form-item label="宠物类型">
          <el-select
            v-model="createForm.petType"
            placeholder="请选择宠物类型"
            style="width: 100%"
            filterable
            allow-create
            default-first-option
          >
            <el-option-group v-for="g in PET_TYPE_GROUPS" :key="g.label" :label="g.label">
              <el-option v-for="o in g.options" :key="o.value" :label="o.label" :value="o.value">
                <span style="float: left">{{ o.emoji ? `${o.emoji} ` : "" }}{{ o.label }}</span>
              </el-option>
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="骨灰盒编号" prop="urnNo">
          <el-input v-model="createForm.urnNo" maxlength="50" />
        </el-form-item>
        <el-form-item label="寄存日期" prop="storageDate">
          <el-date-picker
            v-model="createForm.storageDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            placeholder="请选择寄存日期"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="寄存期限(月)" prop="storagePeriod">
          <el-input-number v-model="createForm.storagePeriod" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="寄存位置">
          <el-input v-model="createForm.storageLocation" maxlength="100" placeholder="如：A区-3排-12柜" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailDialogVisible" title="寄存详情" width="760px" @close="detail = null">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText || statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="宠物名">{{ detail.petName }}</el-descriptions-item>
        <el-descriptions-item label="宠物类型">{{ getPetTypeLabel(detail.petType) || detail.petType || "—" }}</el-descriptions-item>
        <el-descriptions-item label="骨灰盒编号">{{ detail.urnNo }}</el-descriptions-item>
        <el-descriptions-item label="期限(月)">{{ detail.storagePeriod }}</el-descriptions-item>
        <el-descriptions-item label="寄存日期">{{ detail.storageDate }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ detail.expiryDate }}</el-descriptions-item>
        <el-descriptions-item label="寄存位置" :span="2">{{ detail.storageLocation || "—" }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || "—" }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import { urnStorageApi, type UrnStorageCreateDTO, type UrnStorageVO } from "@/api/urnStorage";
import { orderApi, type OrderVO } from "@/api/order";
import dayjs from "dayjs";
import { PET_TYPE_GROUPS, getPetTypeLabel } from "@/constants/petTypes";

const router = useRouter();

function goBack() {
  router.back();
}

const loading = ref(false);
const list = ref<UrnStorageVO[]>([]);

const query = reactive({
  status: undefined as number | undefined,
  petName: "",
  urnNo: "",
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

function resetToFirstPageAndLoad() {
  pagination.current = 1;
  loadList();
}

function resetQuery() {
  query.status = undefined;
  query.petName = "";
  query.urnNo = "";
  resetToFirstPageAndLoad();
}

function handleSizeChange(size: number) {
  pagination.pageSize = size;
  pagination.current = 1;
  loadList();
}

function handlePageChange(page: number) {
  pagination.current = page;
  loadList();
}

function statusText(status: number) {
  switch (status) {
    case -1:
      return "待审批";
    case 0:
      return "寄存中";
    case 1:
      return "已取回";
    case 2:
      return "已到期";
    case 3:
      return "已拒绝";
    default:
      return "未知";
  }
}

function statusTagType(status: number) {
  switch (status) {
    case -1:
      return "warning";
    case 0:
      return "success";
    case 1:
      return "info";
    case 2:
      return "danger";
    case 3:
      return "danger";
    default:
      return "info";
  }
}

function canRetrieve(status: number) {
  return status === 0 || status === 2;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await urnStorageApi.getUrnStorageList({
      status: query.status,
      petName: query.petName || undefined,
      urnNo: query.urnNo || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    });
    list.value = res.data?.records || [];
    pagination.total = res.data?.total || 0;
  } catch (e: any) {
    list.value = [];
    pagination.total = 0;
    ElMessage.error(e?.message || "加载寄存记录失败");
  } finally {
    loading.value = false;
  }
}

// 详情
const detailDialogVisible = ref(false);
const detail = ref<UrnStorageVO | null>(null);

async function openDetail(id: number) {
  try {
    const res = await urnStorageApi.getUrnStorageDetail(id);
    detail.value = res.data;
    detailDialogVisible.value = true;
  } catch (e: any) {
    ElMessage.error(e?.message || "获取详情失败");
  }
}

// 取回
async function retrieve(id: number) {
  try {
    await ElMessageBox.confirm("确定要取回骨灰吗？取回后将标记为“已取回”。", "取回确认", {
      type: "warning",
      confirmButtonText: "确定取回",
      cancelButtonText: "取消",
    });
    await urnStorageApi.retrieveUrnStorage(id);
    ElMessage.success("操作成功");
    await loadList();
  } catch {
    // ignore
  }
}

// 撤销（删除记录，仅 -1 待审批）
async function remove(id: number) {
  try {
    await ElMessageBox.confirm("确定撤销该寄存申请吗？", "撤销确认", {
      type: "warning",
      confirmButtonText: "确定撤销",
      cancelButtonText: "取消",
    });
    await urnStorageApi.deleteUrnStorage(id);
    ElMessage.success("撤销成功");
    await loadList();
  } catch {
    // ignore
  }
}

// 创建
const createDialogVisible = ref(false);
const creating = ref(false);
const createFormRef = ref<any>(null);
const myOrders = ref<OrderVO[]>([]);
const editingId = ref<number | null>(null);

const isEditing = ref(false);
const createDialogTitle = ref("申请骨灰寄存");

const createForm = reactive<UrnStorageCreateDTO>({
  orderId: 0,
  petName: "",
  petType: "",
  urnNo: "",
  storageDate: "",
  storagePeriod: 12,
  storageLocation: "",
  remark: "",
});

const createRules = {
  orderId: [{ required: true, message: "请选择订单", trigger: "change" }],
  petName: [{ required: true, message: "请输入宠物名", trigger: "blur" }],
  urnNo: [{ required: true, message: "请输入骨灰盒编号", trigger: "blur" }],
  storageDate: [{ required: true, message: "请选择寄存日期", trigger: "change" }],
  storagePeriod: [{ required: true, message: "请输入寄存期限", trigger: "change" }],
};

async function loadMyOrders() {
  try {
    const res = await orderApi.getOrderList({ pageNum: 1, pageSize: 50 });
    myOrders.value = res.data?.records || [];
  } catch {
    myOrders.value = [];
  }
}

function openCreate() {
  // 先拉订单列表，便于选择
  loadMyOrders();
  isEditing.value = false;
  editingId.value = null;
  createDialogTitle.value = "申请骨灰寄存";
  Object.assign(createForm, {
    orderId: 0,
    petName: "",
    petType: "",
    urnNo: "",
    storageDate: dayjs().format("YYYY-MM-DD"),
    storagePeriod: 12,
    storageLocation: "",
    remark: "",
  });
  createDialogVisible.value = true;
}

function openEdit(row: UrnStorageVO) {
  loadMyOrders();
  isEditing.value = true;
  editingId.value = row.id;
  createDialogTitle.value = "编辑寄存申请";
  Object.assign(createForm, {
    orderId: row.orderId,
    petName: row.petName || "",
    petType: row.petType || "",
    urnNo: row.urnNo || "",
    storageDate: row.storageDate,
    storagePeriod: row.storagePeriod || 12,
    storageLocation: row.storageLocation || "",
    remark: row.remark || "",
  });
  createDialogVisible.value = true;
}

function handleCreateClose() {
  createFormRef.value?.clearValidate?.();
}

function handleOrderSelected(orderId: number) {
  const order = myOrders.value.find((o) => o.id === orderId);
  if (!order) return;
  // 自动带入宠物信息
  createForm.petName = order.petName || createForm.petName;
  createForm.petType = order.petType || createForm.petType;
}

async function submitCreate() {
  if (!createFormRef.value) return;
  try {
    await createFormRef.value.validate();
  } catch {
    return;
  }

  if (!createForm.orderId) {
    ElMessage.warning("请选择订单");
    return;
  }

  creating.value = true;
  try {
    if (editingId.value) {
      await urnStorageApi.updateUrnStorage(editingId.value, createForm);
      ElMessage.success("更新成功");
    } else {
      await urnStorageApi.createUrnStorage(createForm);
      ElMessage.success("提交成功，等待审批");
    }
    createDialogVisible.value = false;
    await loadList();
  } catch (e: any) {
    ElMessage.error(e?.message || "提交失败");
  } finally {
    creating.value = false;
  }
}

onMounted(() => {
  loadList();
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

.urn-storage-container {
  padding: 30px 20px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 100px);
  background: #ffffff;

  :deep(.el-page-header) {
    margin-bottom: 24px;

    .el-page-header__content {
      color: $text-primary;
      font-weight: 700;
      font-size: 18px;
    }
  }

  .content-wrapper {
    margin-top: 20px;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .action-card {
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    :deep(.el-card__body) {
      padding: 20px;
    }

    .action-bar {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;

      :deep(.el-button--primary) {
        background-color: $primary-color;
        border-color: $primary-color;
        border-radius: 8px;
        font-weight: 500;

        &:hover {
          filter: brightness(1.05);
        }
      }
    }

    .filter-form {
      margin-top: 8px;

      :deep(.el-form-item__label) {
        color: $text-secondary;
        font-weight: 500;
      }

      :deep(.el-input__wrapper),
      :deep(.el-select .el-input__wrapper) {
        border-radius: 8px;
      }

      :deep(.el-button--primary) {
        background-color: $primary-color;
        border-color: $primary-color;
        border-radius: 8px;
      }
    }
  }

  // 优化表格卡片
  :deep(.el-card) {
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .el-card__body {
      padding: 20px;
    }
  }

  // 优化表格样式
  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;

    .el-table__header {
      th {
        background-color: $bg-light;
        color: $text-primary;
        font-weight: 600;
      }
    }

    .el-table__body {
      tr:hover > td {
        background-color: rgba(64, 158, 255, 0.05);
      }
    }
  }

  // 优化标签样式
  :deep(.el-tag) {
    border-radius: 999px;
    font-weight: 500;
  }

  // 优化按钮样式
  :deep(.el-button) {
    border-radius: 6px;
    font-weight: 500;

    &.el-button--warning {
      background-color: $warning-color;
      border-color: $warning-color;
    }

    &.el-button--danger {
      color: $danger-color;
      border-color: $danger-color;

      &:hover {
        background-color: rgba(245, 108, 108, 0.1);
      }
    }
  }

  .pager {
    margin-top: 20px;
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

  .form-tip {
    margin-top: 8px;
    font-size: 12px;
    color: $text-light;
    line-height: 1.6;
    padding: 8px 12px;
    background: $bg-light;
    border-radius: 6px;
    border-left: 3px solid $primary-color;
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

    .el-input__wrapper,
    .el-select .el-input__wrapper,
    .el-textarea__inner,
    .el-date-editor,
    .el-input-number {
      border-radius: 8px;
    }

    .el-button--primary {
      background-color: $primary-color;
      border-color: $primary-color;
      border-radius: 8px;
    }
  }

  // 优化描述列表样式
  :deep(.el-descriptions) {
    .el-descriptions__label.is-bordered-label {
      background: $bg-light;
      color: $text-secondary;
      font-weight: 600;
    }

    .el-descriptions__content.is-bordered-content {
      color: $text-primary;
    }
  }

  // 优化空状态
  :deep(.el-empty) {
    padding: 60px 0;
  }
}
</style>


