<template>
  <el-dialog
    v-model="visible"
    title="添加服务记录"
    width="600px"
    :before-close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="服务时间" prop="serviceTime">
        <el-date-picker
          v-model="form.serviceTime"
          type="datetime"
          placeholder="选择服务时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="服务内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          placeholder="请输入服务内容"
        />
      </el-form-item>
      <el-form-item label="服务人员" prop="staffName">
        <el-input v-model="form.staffName" placeholder="请输入服务人员姓名" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="请输入备注信息（选填）"
        />
      </el-form-item>
      <el-form-item label="上传图片" prop="images">
        <el-upload
          v-model:file-list="fileList"
          action="#"
          list-type="picture-card"
          :auto-upload="false"
          :limit="5"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="submitRecord">保存记录</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from "vue";
import {
  ElMessage,
  type FormInstance,
  type UploadUserFile,
} from "element-plus";
import { Plus } from "@element-plus/icons-vue";

interface ServiceRecordForm {
  serviceTime: string;
  content: string;
  staffName: string;
  remark: string;
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
const fileList = ref<UploadUserFile[]>([]);
const form = reactive<ServiceRecordForm>({
  serviceTime: "",
  content: "",
  staffName: "",
  remark: "",
});

const rules = {
  serviceTime: [
    { required: true, message: "请选择服务时间", trigger: "change" },
  ],
  content: [{ required: true, message: "请输入服务内容", trigger: "blur" }],
  staffName: [
    { required: true, message: "请输入服务人员姓名", trigger: "blur" },
  ],
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
  form.serviceTime = "";
  form.content = "";
  form.staffName = "";
  form.remark = "";
  fileList.value = [];
};

const handleClose = () => {
  visible.value = false;
};

const submitRecord = async () => {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();

    // 这里应该调用添加服务记录API
    // await serviceRecordApi.createRecord({
    //   orderId: props.orderId,
    //   ...form,
    //   images: fileList.value
    // });

    ElMessage.success("服务记录已添加");
    emit("success");
    handleClose();
  } catch (error) {
    console.error("添加服务记录失败:", error);
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
