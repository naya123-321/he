<template>
  <div class="create-order-container">
    <el-page-header @back="$router.go(-1)" content="创建服务订单" />

    <el-card class="order-form-card">
      <el-steps :active="activeStep" finish-status="success">
        <el-step title="选择服务" />
        <el-step title="填写宠物信息" />
        <el-step title="预约信息" />
        <el-step title="确认订单" />
      </el-steps>

      <!-- 步骤1: 选择服务 -->
      <div v-if="activeStep === 0" class="step-content">
        <h3>选择服务类型</h3>
        <el-row :gutter="20">
          <el-col
            v-for="service in serviceTypes"
            :key="service.id"
            :span="8"
            style="margin-bottom: 20px"
          >
            <el-card
              :class="[
                'service-card',
                { selected: form.serviceTypeId === service.id },
              ]"
              @click="selectService(service)"
              shadow="hover"
            >
              <div class="service-header">
                <h4>{{ service.name }}</h4>
                <span class="price">¥{{ service.price }}</span>
              </div>
              <p class="description">{{ service.description }}</p>
              <div class="service-footer">
                <span class="duration">约{{ service.duration }}分钟</span>
                <el-tag v-if="form.serviceTypeId === service.id" type="success">
                  <el-icon><Check /></el-icon> 已选择
                </el-tag>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤2: 宠物信息 -->
      <div v-if="activeStep === 1" class="step-content">
        <h3>填写宠物信息</h3>
        <el-form ref="petFormRef" :model="form" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="宠物姓名" prop="petName" required>
                <el-input v-model="form.petName" placeholder="请输入宠物名字" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="宠物类型" prop="petType" required>
                <el-select v-model="form.petType" placeholder="请选择">
                  <el-option label="狗狗" value="狗" />
                  <el-option label="猫咪" value="猫" />
                  <el-option label="兔子" value="兔" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="宠物品种" prop="petBreed">
                <el-input
                  v-model="form.petBreed"
                  placeholder="如：金毛、波斯猫"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="年龄" prop="petAge">
                <el-input-number v-model="form.petAge" :min="0" :max="50" />
                <span style="margin-left: 5px">岁</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="体重" prop="petWeight">
                <el-input-number
                  v-model="form.petWeight"
                  :min="0"
                  :max="100"
                  :precision="2"
                />
                <span style="margin-left: 5px">kg</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="离世日期" prop="deathDate" required>
                <el-date-picker
                  v-model="form.deathDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="离世原因" prop="deathCause">
            <el-input
              v-model="form.deathCause"
              type="textarea"
              :rows="3"
              placeholder="可选填，帮助我们更好地提供服务"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤3: 预约信息 -->
      <div v-if="activeStep === 2" class="step-content">
        <h3>填写预约信息</h3>
        <el-form ref="appointmentFormRef" :model="form" label-width="120px">
          <el-form-item label="预约时间" prop="appointmentTime" required>
            <el-date-picker
              v-model="form.appointmentTime"
              type="datetime"
              placeholder="选择日期和时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledDate"
              :shortcuts="timeShortcuts"
            />
            <div class="form-tip">预约时间需提前至少2小时</div>
          </el-form-item>

          <el-form-item label="服务地址" prop="address" required>
            <el-input
              v-model="form.address"
              type="textarea"
              :rows="3"
              placeholder="请填写详细地址"
            />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactName" required>
                <el-input
                  v-model="form.contactName"
                  placeholder="请输入联系人姓名"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone" required>
                <el-input
                  v-model="form.contactPhone"
                  placeholder="请输入手机号"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="特殊要求" prop="specialRequirements">
            <el-input
              v-model="form.specialRequirements"
              type="textarea"
              :rows="3"
              placeholder="可选填，如特殊仪式要求等"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤4: 确认订单 -->
      <div v-if="activeStep === 3" class="step-content">
        <h3>确认订单信息</h3>

        <el-descriptions title="服务信息" :column="2" border>
          <el-descriptions-item label="服务类型">{{
            selectedService?.name
          }}</el-descriptions-item>
          <el-descriptions-item label="价格"
            >¥{{ selectedService?.price }}</el-descriptions-item
          >
          <el-descriptions-item label="预计时长"
            >{{ selectedService?.duration }}分钟</el-descriptions-item
          >
          <el-descriptions-item label="服务描述">{{
            selectedService?.description
          }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions
          title="宠物信息"
          :column="2"
          border
          style="margin-top: 20px"
        >
          <el-descriptions-item label="宠物姓名">{{
            form.petName
          }}</el-descriptions-item>
          <el-descriptions-item label="宠物类型">{{
            form.petType
          }}</el-descriptions-item>
          <el-descriptions-item label="品种">{{
            form.petBreed || "未填写"
          }}</el-descriptions-item>
          <el-descriptions-item label="年龄">{{
            form.petAge ? form.petAge + "岁" : "未填写"
          }}</el-descriptions-item>
          <el-descriptions-item label="体重">{{
            form.petWeight ? form.petWeight + "kg" : "未填写"
          }}</el-descriptions-item>
          <el-descriptions-item label="离世日期">{{
            form.deathDate
          }}</el-descriptions-item>
          <el-descriptions-item label="离世原因" :span="2">{{
            form.deathCause || "未填写"
          }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions
          title="预约信息"
          :column="2"
          border
          style="margin-top: 20px"
        >
          <el-descriptions-item label="预约时间">{{
            form.appointmentTime
          }}</el-descriptions-item>
          <el-descriptions-item label="服务地址">{{
            form.address
          }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{
            form.contactName
          }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{
            form.contactPhone
          }}</el-descriptions-item>
          <el-descriptions-item label="特殊要求" :span="2">{{
            form.specialRequirements || "无"
          }}</el-descriptions-item>
        </el-descriptions>

        <div class="order-summary">
          <h4>
            订单总金额：<span class="total-amount"
              >¥{{ selectedService?.price || 0 }}</span
            >
          </h4>
          <p class="agreement">
            <el-checkbox v-model="agreeAgreement">
              我已阅读并同意
              <el-link type="primary" @click="showAgreement = true"
                >《服务协议》</el-link
              >
            </el-checkbox>
          </p>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="step-actions">
        <el-button v-if="activeStep > 0" @click="prevStep">上一步</el-button>
        <el-button
          v-if="activeStep < 3"
          type="primary"
          @click="nextStep"
          :disabled="!canNextStep"
        >
          下一步
        </el-button>
        <el-button
          v-if="activeStep === 3"
          type="success"
          @click="submitOrder"
          :loading="submitting"
          :disabled="!agreeAgreement"
        >
          提交订单
        </el-button>
      </div>
    </el-card>

    <!-- 服务协议弹窗 -->
    <el-dialog v-model="showAgreement" title="服务协议" width="70%">
      <div class="agreement-content">
        <!-- 协议内容 -->
        <p>请在此处填写完整的服务协议内容...</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="showAgreement = false"
          >我已阅读</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElLoading, ElMessageBox } from "element-plus";
import { Check } from "@element-plus/icons-vue";
import { useOrderStore } from "@/store/order";
import { orderApi, serviceTypeApi } from "@/api/order";
import dayjs from "dayjs";
import type { ServiceTypeVO, OrderCreateDTO } from "@/api/order";

// 组件实例
const router = useRouter();
const orderStore = useOrderStore();

// 表单引用
const petFormRef = ref();
const appointmentFormRef = ref();
const confirmFormRef = ref();

// 状态管理
const activeStep = ref(0);
const submitting = ref(false);
const showAgreement = ref(false);
const agreeAgreement = ref(false);
const imageDialogVisible = ref(false);
const isResizing = ref(false);
const initialWidth = ref(0);
const initialHeight = ref(0);
const initialX = ref(0);
const initialY = ref(0);
const serviceTypes = ref<ServiceTypeVO[]>([]);

// 表单数据
const form = reactive<OrderCreateDTO>({
  serviceTypeId: 0,
  petName: "",
  petType: "",
  petBreed: "",
  petAge: 0,
  petWeight: 0,
  deathDate: "",
  deathCause: "",
  appointmentTime: "",
  address: "",
  contactName: "",
  contactPhone: "",
  specialRequirements: "",
});

// 计算属性
const selectedService = computed(() => {
  return serviceTypes.value.find((s) => s.id === form.serviceTypeId);
});

// 时间选择器快捷选项
const timeShortcuts = [
  {
    text: "2小时后",
    value: () => {
      return dayjs().add(2, "hour").format("YYYY-MM-DD HH:mm:ss");
    },
  },
  {
    text: "明天上午9点",
    value: () => {
      return dayjs()
        .add(1, "day")
        .hour(9)
        .minute(0)
        .format("YYYY-MM-DD HH:mm:ss");
    },
  },
  {
    text: "明天下午2点",
    value: () => {
      return dayjs()
        .add(1, "day")
        .hour(14)
        .minute(0)
        .format("YYYY-MM-DD HH:mm:ss");
    },
  },
];

const canNextStep = computed(() => {
  switch (activeStep.value) {
    case 0:
      return form.serviceTypeId > 0;
    case 1:
      return form.petName && form.petType && form.deathDate;
    case 2:
      return (
        form.appointmentTime &&
        form.address &&
        form.contactName &&
        form.contactPhone
      );
    default:
      return true;
  }
});

// 生命周期
onMounted(async () => {
  await fetchServiceTypes();

  // 检查URL参数中是否有套餐类型
  const route = useRoute();
  const packageType = route.query.type as string;

  // 根据套餐类型自动选择对应的服务
  if (packageType && serviceTypes.value.length > 0) {
    let targetServiceId: number | null = null;

    // 根据套餐类型匹配服务
    switch (packageType) {
      case "basic":
        // 基础套餐：选择价格最低的服务
        targetServiceId = serviceTypes.value.reduce((min, service) =>
          service.price < min.price ? service : min
        ).id;
        break;
      case "standard":
        // 标准套餐：选择价格中等的服务
        const sortedServices = [...serviceTypes.value].sort(
          (a, b) => a.price - b.price
        );
        const middleIndex = Math.floor(sortedServices.length / 2);
        targetServiceId = sortedServices[middleIndex].id;
        break;
      case "premium":
        // 高级套餐：选择价格最高的服务
        targetServiceId = serviceTypes.value.reduce((max, service) =>
          service.price > max.price ? service : max
        ).id;
        break;
    }

    // 如果找到了匹配的服务，自动选择
    if (targetServiceId) {
      form.serviceTypeId = targetServiceId;
    }
  }
});

// 方法
async function fetchServiceTypes() {
  try {
    const res = await serviceTypeApi.getServiceTypes();
    if (res.data.code === 200) {
      serviceTypes.value = res.data.data;
      // 默认选择第一个服务
      if (serviceTypes.value.length > 0) {
        form.serviceTypeId = serviceTypes.value[0].id;
      }
    }
  } catch (error) {
    console.error("获取服务类型失败:", error);
    ElMessage.error("获取服务类型失败，请刷新页面重试");
  }
}

function selectService(service: ServiceTypeVO) {
  form.serviceTypeId = service.id;
}

function disabledDate(date: Date) {
  // 只能选择今天及以后的日期
  return date < new Date(new Date().setHours(0, 0, 0, 0));
}

function formatDate(dateString: string) {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

async function prevStep() {
  activeStep.value--;
}

async function nextStep() {
  submitting.value = true;
  try {
    if (activeStep.value === 0) {
      // 验证服务是否已选择
      if (!form.serviceTypeId) {
        ElMessage.warning("请选择服务类型");
        return;
      }
      activeStep.value++;
    } else if (activeStep.value === 1) {
      // 验证宠物信息表单
      const valid = await petFormRef.value.validate();
      if (valid) {
        activeStep.value++;
      }
    } else if (activeStep.value === 2) {
      // 验证预约信息表单
      const valid = await appointmentFormRef.value.validate();
      if (valid) {
        activeStep.value++;
      }
    }
  } catch (error) {
    console.error("步骤验证失败:", error);
  } finally {
    submitting.value = false;
  }
}

async function submitOrder() {
  submitting.value = true;
  const loading = ElLoading.service({
    lock: true,
    text: "正在创建订单...",
    background: "rgba(0, 0, 0, 0.7)",
  });

  try {
    const res = await orderStore.createOrder(form);
    if (res) {
      ElMessage.success("订单创建成功！");

      // 如果服务类型包含纪念册制作，询问是否立即创建纪念册
      const selectedService = serviceTypes.value.find(
        (s) => s.id === form.serviceTypeId
      );
      if (selectedService && selectedService.name.includes("纪念册")) {
        try {
          await ElMessageBox.confirm(
            "是否立即为您的宠物创建纪念册？",
            "创建纪念册",
            {
              confirmButtonText: "立即创建",
              cancelButtonText: "稍后创建",
              type: "info",
            }
          );

          // 跳转到纪念册创建页面，并传递订单ID
          router.push({
            path: "/memorial/create",
            query: { orderId: res.id },
          });
        } catch (error) {
          // 用户选择稍后创建，跳转到订单详情页
          router.push({ path: "/order/detail", query: { id: res.id } });
        }
      } else {
        // 不包含纪念册制作服务，直接跳转到订单详情页
        router.push({ path: "/order/detail", query: { id: res.id } });
      }
    }
  } catch (error) {
    console.error("创建订单失败:", error);
    ElMessage.error("创建订单失败，请稍后重试");
  } finally {
    submitting.value = false;
    loading.close();
  }
}

const loadImages = async () => {
  try {
    // 调用获取图片列表的API
    const res = await memorialApi.getMemorialImages(props.memorialId);
    imageList.value = res.data.data;
  } catch (error) {
    console.error("加载图片失败:", error);
    ElMessage.error("图片加载失败，请重试");
  }
};

const editElement = (element: any) => {
  selectedElement.value = element;
  // 如果是文本，弹出编辑器
  if (element.type === "text") {
    ElMessageBox.prompt(element.content, "编辑文本", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "input",
    })
      .then(({ value }) => {
        element.content = value;
      })
      .catch(() => {
        // 取消编辑
      });
  }
};

const startResize = (event: MouseEvent) => {
  if (!selectedElement.value) return;

  event.preventDefault();
  isResizing.value = true;
  initialWidth.value = selectedElement.value.width;
  initialHeight.value = selectedElement.value.height;
  initialX.value = event.clientX;
  initialY.value = event.clientY;

  const onMouseMove = (e: MouseEvent) => {
    if (!isResizing.value) return;

    const deltaX = e.clientX - initialX.value;
    const deltaY = e.clientY - initialY.value;

    selectedElement.value.width = initialWidth.value + deltaX;
    selectedElement.value.height = initialHeight.value + deltaY;
  };

  const onMouseUp = () => {
    isResizing.value = false;
    window.removeEventListener("mousemove", onMouseMove);
    window.removeEventListener("mouseup", onMouseUp);
  };

  window.addEventListener("mousemove", onMouseMove);
  window.addEventListener("mouseup", onMouseUp);
};
</script>

<style scoped lang="scss">
.create-order-container {
  padding: 20px;

  .order-form-card {
    margin-top: 20px;
    padding: 20px;

    .step-content {
      margin: 40px 0;

      .service-card {
        cursor: pointer;
        transition: all 0.3s;

        &.selected {
          border-color: #409eff;
          box-shadow: 0 0 8px rgba(64, 158, 255, 0.3);
        }

        .service-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          h4 {
            margin: 0;
            color: #333;
          }

          .price {
            font-size: 18px;
            font-weight: bold;
            color: #e6a23c;
          }
        }

        .description {
          color: #666;
          font-size: 14px;
          line-height: 1.5;
          margin-bottom: 10px;
          height: 40px;
          overflow: hidden;
        }

        .service-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .duration {
            color: #999;
            font-size: 12px;
          }
        }
      }

      .form-tip {
        font-size: 12px;
        color: #999;
        margin-top: 5px;
      }

      .order-summary {
        margin-top: 30px;
        padding: 20px;
        background: #f8f9fa;
        border-radius: 4px;

        .total-amount {
          font-size: 24px;
          color: #e6a23c;
          font-weight: bold;
        }

        .agreement {
          margin-top: 20px;
        }
      }
    }

    .step-actions {
      margin-top: 40px;
      text-align: center;
    }
  }

  .agreement-content {
    max-height: 400px;
    overflow-y: auto;
    line-height: 1.6;
  }
}

.order-form-card {
  margin-top: 20px;
}

.step-content {
  margin: 30px 0;
  min-height: 400px;
}

.step-navigation {
  margin-top: 30px;
  text-align: right;
}

.service-card {
  cursor: pointer;
  transition: all 0.3s;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.service-card.selected {
  border-color: #409eff;
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.3);
}

.service-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.description {
  color: #606266;
  margin-bottom: 15px;
  flex-grow: 1;
}

.service-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed #f0f0f0;
}

.duration {
  color: #909399;
  font-size: 12px;
}

.order-summary-card {
  padding: 20px;
}

.summary-item {
  display: flex;
  margin-bottom: 15px;
}

.summary-item:last-child {
  margin-bottom: 0;
}

.label {
  width: 120px;
  color: #909399;
  text-align: right;
  padding-right: 20px;
}

.value {
  flex: 1;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
</style>
