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
        <el-descriptions-item label="宠物类型">{{ detail.petType || "—" }}</el-descriptions-item>
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
import { PET_TYPE_GROUPS } from "@/constants/petTypes";

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
.urn-storage-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;

  .content-wrapper {
    margin-top: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .action-card {
    .action-bar {
      display: flex;
      gap: 10px;
      margin-bottom: 12px;
    }

    .filter-form {
      margin-top: 6px;
    }
  }

  .pager {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .form-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
  }
}
</style>


