<template>
  <div class="urn-storage-management-container">
    <el-page-header @back="goBack" content="骨灰寄存管理（管理员）" />

    <div class="content-wrapper">
      <!-- 筛选栏 -->
      <el-card shadow="never" class="filter-card">
        <el-form :inline="true" class="filter-form" @submit.prevent>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="handleSearch">
              <el-option label="待审批" :value="-1" />
              <el-option label="寄存中" :value="0" />
              <el-option label="已取回" :value="1" />
              <el-option label="已到期" :value="2" />
              <el-option label="已拒绝" :value="3" />
            </el-select>
          </el-form-item>

          <el-form-item label="宠物名">
            <el-input
              v-model="query.petName"
              placeholder="模糊匹配"
              clearable
              style="width: 180px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </el-form-item>

          <el-form-item label="骨灰盒编号">
            <el-input
              v-model="query.urnNo"
              placeholder="模糊匹配"
              clearable
              style="width: 180px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </el-form-item>

          <el-form-item label="订单ID">
            <el-input-number
              v-model="query.orderId"
              :min="1"
              :controls="false"
              placeholder="精确匹配"
              style="width: 160px"
              @change="handleSearch"
            />
          </el-form-item>

          <el-form-item label="宠主ID">
            <el-input-number
              v-model="query.userId"
              :min="1"
              :controls="false"
              placeholder="精确匹配"
              style="width: 160px"
              @change="handleSearch"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 列表 -->
      <el-card shadow="never">
        <el-table v-loading="loading" :data="list" stripe border style="width: 100%">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
          <el-table-column label="宠主" min-width="160">
            <template #default="{ row }">
              <div class="user-cell">
                <div class="user-name">{{ row.userName || "-" }}</div>
                <div class="user-phone">{{ row.userPhone || "-" }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="petName" label="宠物名" width="120" />
          <el-table-column prop="petType" label="宠物类型" width="120" show-overflow-tooltip />
          <el-table-column prop="urnNo" label="骨灰盒编号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="storageLocation" label="寄存位置" min-width="160" show-overflow-tooltip />
          <el-table-column label="寄存/到期" width="210">
            <template #default="{ row }">
              <div>{{ row.storageDate || "-" }}</div>
              <div class="muted">到期：{{ row.expiryDate || "-" }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">
                {{ row.statusText || statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="360" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>

              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>

              <el-button
                v-if="row.status === -1"
                link
                type="success"
                :loading="rowActionLoading[row.id]?.approving === true"
                @click="approve(row.id, true)"
              >
                通过
              </el-button>
              <el-button
                v-if="row.status === -1"
                link
                type="danger"
                :loading="rowActionLoading[row.id]?.rejecting === true"
                @click="approve(row.id, false)"
              >
                拒绝
              </el-button>

              <el-button
                v-if="canRetrieve(row.status)"
                link
                type="warning"
                :loading="rowActionLoading[row.id]?.retrieving === true"
                @click="retrieve(row.id)"
              >
                标记取回
              </el-button>

              <el-button
                link
                type="danger"
                :loading="rowActionLoading[row.id]?.deleting === true"
                @click="remove(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailDialogVisible" title="寄存详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText }}</el-descriptions-item>
        <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="宠主">{{ detail.userName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.userPhone || "-" }}</el-descriptions-item>
        <el-descriptions-item label="宠物名">{{ detail.petName }}</el-descriptions-item>
        <el-descriptions-item label="宠物类型">{{ detail.petType || "-" }}</el-descriptions-item>
        <el-descriptions-item label="骨灰盒编号">{{ detail.urnNo }}</el-descriptions-item>
        <el-descriptions-item label="寄存位置">{{ detail.storageLocation || "-" }}</el-descriptions-item>
        <el-descriptions-item label="寄存日期">{{ detail.storageDate }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ detail.expiryDate }}</el-descriptions-item>
        <el-descriptions-item label="寄存期限（月）">{{ detail.storagePeriod }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || "-" }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="editDialogVisible" title="编辑寄存记录" width="620px" @close="handleEditClose">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item label="订单ID" prop="orderId">
          <el-input-number v-model="editForm.orderId" :min="0" :controls="false" style="width: 100%" disabled />
        </el-form-item>
        <el-form-item label="宠物名" prop="petName">
          <el-input v-model="editForm.petName" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="宠物类型" prop="petType">
          <el-select
            v-model="editForm.petType"
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
          <el-input v-model="editForm.urnNo" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="寄存日期" prop="storageDate">
          <el-date-picker
            v-model="editForm.storageDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="寄存期限（月）" prop="storagePeriod">
          <el-input-number v-model="editForm.storagePeriod" :min="1" :max="120" style="width: 100%" />
        </el-form-item>
        <el-form-item label="寄存位置" prop="storageLocation">
          <el-input v-model="editForm.storageLocation" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox, type FormInstance } from "element-plus";
import { Refresh, Search } from "@element-plus/icons-vue";
import dayjs from "dayjs";
import { urnStorageApi, type UrnStorageCreateDTO, type UrnStorageVO } from "@/api/urnStorage";
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
  orderId: undefined as number | undefined,
  userId: undefined as number | undefined,
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

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

function formatDateTime(dateStr?: string) {
  if (!dateStr) return "-";
  try {
    return dayjs(dateStr).format("YYYY-MM-DD HH:mm:ss");
  } catch {
    return dateStr;
  }
}

async function loadList() {
  loading.value = true;
  try {
    const res = await urnStorageApi.getUrnStorageList({
      userId: query.userId ?? undefined,
      status: query.status,
      petName: query.petName || undefined,
      urnNo: query.urnNo || undefined,
      orderId: query.orderId ?? undefined,
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

function handleSearch() {
  pagination.current = 1;
  loadList();
}

function handleReset() {
  query.status = undefined;
  query.petName = "";
  query.urnNo = "";
  query.orderId = undefined;
  query.userId = undefined;
  handleSearch();
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

const rowActionLoading = reactive<Record<number, { approving?: boolean; rejecting?: boolean; retrieving?: boolean; deleting?: boolean }>>({});
function setRowLoading(id: number, key: "approving" | "rejecting" | "retrieving" | "deleting", val: boolean) {
  rowActionLoading[id] = rowActionLoading[id] || {};
  rowActionLoading[id][key] = val;
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

// 审批
async function approve(id: number, approved: boolean) {
  const actionText = approved ? "通过" : "拒绝";
  try {
    await ElMessageBox.confirm(`确定要${actionText}该寄存申请吗？`, "审批确认", {
      type: "warning",
      confirmButtonText: `确定${actionText}`,
      cancelButtonText: "取消",
    });
    setRowLoading(id, approved ? "approving" : "rejecting", true);
    await urnStorageApi.approveUrnStorageRequest(id, approved);
    ElMessage.success("操作成功");
    await loadList();
  } catch {
    // ignore
  } finally {
    setRowLoading(id, approved ? "approving" : "rejecting", false);
  }
}

// 取回
async function retrieve(id: number) {
  try {
    await ElMessageBox.confirm("确定要标记为“已取回”吗？", "确认", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });
    setRowLoading(id, "retrieving", true);
    await urnStorageApi.retrieveUrnStorage(id);
    ElMessage.success("操作成功");
    await loadList();
  } catch {
    // ignore
  } finally {
    setRowLoading(id, "retrieving", false);
  }
}

// 删除
async function remove(id: number) {
  try {
    await ElMessageBox.confirm("确定要删除该寄存记录吗？该操作不可恢复。", "删除确认", {
      type: "warning",
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
    });
    setRowLoading(id, "deleting", true);
    await urnStorageApi.deleteUrnStorage(id);
    ElMessage.success("删除成功");
    await loadList();
  } catch {
    // ignore
  } finally {
    setRowLoading(id, "deleting", false);
  }
}

// 编辑
const editDialogVisible = ref(false);
const submitting = ref(false);
const editFormRef = ref<FormInstance>();
const editingId = ref<number | null>(null);

const editForm = reactive<UrnStorageCreateDTO>({
  orderId: 0,
  petName: "",
  petType: "",
  urnNo: "",
  storageDate: "",
  storagePeriod: 12,
  storageLocation: "",
  remark: "",
});

const editRules = {
  orderId: [{ required: true, message: "订单ID不能为空", trigger: "blur" }],
  petName: [{ required: true, message: "请输入宠物名", trigger: "blur" }],
  urnNo: [{ required: true, message: "请输入骨灰盒编号", trigger: "blur" }],
  storageDate: [{ required: true, message: "请选择寄存日期", trigger: "change" }],
  storagePeriod: [{ required: true, message: "请输入寄存期限", trigger: "change" }],
};

function openEdit(row: UrnStorageVO) {
  editingId.value = row.id;
  Object.assign(editForm, {
    orderId: row.orderId,
    petName: row.petName || "",
    petType: row.petType || "",
    urnNo: row.urnNo || "",
    storageDate: row.storageDate || "",
    storagePeriod: row.storagePeriod || 12,
    storageLocation: row.storageLocation || "",
    remark: row.remark || "",
  });
  editDialogVisible.value = true;
}

function handleEditClose() {
  editFormRef.value?.clearValidate();
}

async function submitEdit() {
  if (!editFormRef.value || !editingId.value) return;
  try {
    await editFormRef.value.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    await urnStorageApi.updateUrnStorage(editingId.value, editForm);
    ElMessage.success("保存成功");
    editDialogVisible.value = false;
    await loadList();
  } catch (e: any) {
    ElMessage.error(e?.message || "保存失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadList();
});
</script>

<style scoped lang="scss">
.urn-storage-management-container {
  padding: 20px;

  .content-wrapper {
    margin-top: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .filter-card {
    .filter-form {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 12px;
      align-items: center;
    }
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .muted {
    color: #909399;
    font-size: 12px;
  }

  .user-cell {
    display: flex;
    flex-direction: column;
    line-height: 1.2;
    .user-name {
      font-weight: 600;
    }
    .user-phone {
      color: #909399;
      font-size: 12px;
    }
  }
}
</style>
