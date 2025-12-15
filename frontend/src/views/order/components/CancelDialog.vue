<template>
  <el-dialog
    v-model="visible"
    title="取消订单"
    width="500px"
    :before-close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="取消原因" prop="reason">
        <el-select
          v-model="form.reason"
          placeholder="请选择取消原因"
          style="width: 100%"
        >
          <el-option label="计划有变" value="计划有变" />
          <el-option label="价格原因" value="价格原因" />
          <el-option label="服务不满意" value="服务不满意" />
          <el-option label="其他原因" value="其他原因" />
        </el-select>
      </el-form-item>
      <el-form-item label="详细说明" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请详细说明取消原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="submitCancel">确认取消</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from "vue";
import { ElMessage, type FormInstance } from "element-plus";

interface CancelForm {
  reason: string;
  description: string;
}

interface Props {
  modelValue: boolean;
  orderId?: string;
}

interface Emits {
  (e: "update:modelValue", value: boolean): void;
  (e: "success"): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const visible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<CancelForm>({
  reason: "",
  description: "",
});

const rules = {
  reason: [{ required: true, message: "请选择取消原因", trigger: "change" }],
  description: [{ required: true, message: "请输入详细说明", trigger: "blur" }],
};

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val;
  }
);

watch(visible, (val) => {
  emit("update:modelValue", val);
  if (!val) {
    resetForm();
  }
});

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }
  form.reason = "";
  form.description = "";
};

const handleClose = () => {
  visible.value = false;
};

const submitCancel = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    // 这里应该调用取消订单API
    // await orderApi.cancelOrder({
    //   orderId: props.orderId,
    //   reason: form.reason,
    //   description: form.description
    // });

    ElMessage.success("订单已取消");
    emit("success");
    handleClose();
  } catch (error) {
    console.error("取消订单失败:", error);
  }
};
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
