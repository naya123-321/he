<template>
  <div class="schedule-container">
    <el-page-header @back="goBack" content="我的日程" />
    
    <div class="content-wrapper">
      <el-calendar v-model="selectedDate" v-loading="loading">
        <template #date-cell="{ data }">
          <div 
            class="calendar-day" 
            :class="{ 'has-appointments': getAppointmentsForDate(data.day).length > 0, 'selected-date': clickedDate === data.day }"
            @click="handleDateClick(data.day)"
          >
            <div class="day-number">{{ data.day.split('-').slice(2).join('-') }}</div>
            <div v-if="getAppointmentsForDate(data.day).length > 0" class="appointments-count">
              {{ getAppointmentsForDate(data.day).length }} 个预约
            </div>
          </div>
        </template>
      </el-calendar>
      
      <div class="appointments-list" v-loading="loading">
        <div class="list-header">
          <h3>{{ getDateTitle() }}</h3>
          <el-button size="small" @click="goToToday">回到今日</el-button>
        </div>
        <el-empty v-if="!loading && selectedDateAppointments.length === 0" :description="getEmptyDescription()" />
        <div v-else-if="selectedDateAppointments.length > 0" class="appointments">
          <div
            v-for="appointment in selectedDateAppointments"
            :key="appointment.id"
            class="appointment-item"
            @click="viewAppointment(appointment.id)"
          >
            <div class="appointment-time">
              <span class="time">{{ appointment.startTime }}</span>
              <span class="duration">{{ appointment.duration }}分钟</span>
            </div>
            <div class="appointment-info">
              <h4>{{ appointment.petName }} <span class="pet-type">({{ getPetTypeLabel(appointment.petType) || appointment.petType }})</span></h4>
              <p>{{ appointment.serviceType }}</p>
              <p class="contact-info">
                <span>联系人：{{ appointment.contactName }}</span>
                <span style="margin-left: 16px;">电话：{{ appointment.contactPhone }}</span>
              </p>
              <p v-if="appointment.address" class="address">地址：{{ appointment.address }}</p>
              <span class="status" :class="getStatusClass(appointment.status)">
                {{ getStatusText(appointment.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElPageHeader, ElCalendar, ElEmpty, ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { orderApi } from '@/api/order';
import { useUserStore } from '@/store/user';
import { getPetTypeLabel } from '@/constants/petTypes';

const router = useRouter();
const userStore = useUserStore();
const selectedDate = ref(new Date());
const clickedDate = ref<string>(dayjs().format('YYYY-MM-DD')); // 点击选中的日期
const appointments = ref<any[]>([]);
const loading = ref(false);

// 选中日期的预约列表
const selectedDateAppointments = computed(() => {
  return getAppointmentsForDate(clickedDate.value);
});

// 处理日期点击
const handleDateClick = (date: string) => {
  clickedDate.value = date;
  // 更新日历的选中日期
  selectedDate.value = new Date(date);
};

// 回到今日
const goToToday = () => {
  const today = dayjs().format('YYYY-MM-DD');
  clickedDate.value = today;
  selectedDate.value = new Date();
};

// 获取日期标题
const getDateTitle = () => {
  const today = dayjs().format('YYYY-MM-DD');
  const clicked = clickedDate.value;
  
  if (clicked === today) {
    return '今日预约';
  }
  
  const date = dayjs(clicked);
  const todayDate = dayjs();
  
  if (date.isSame(todayDate, 'day')) {
    return '今日预约';
  } else if (date.isSame(todayDate.subtract(1, 'day'), 'day')) {
    return '昨日预约';
  } else if (date.isSame(todayDate.add(1, 'day'), 'day')) {
    return '明日预约';
  } else {
    return `${date.format('YYYY年MM月DD日')} 预约`;
  }
};

// 获取空状态描述
const getEmptyDescription = () => {
  const today = dayjs().format('YYYY-MM-DD');
  if (clickedDate.value === today) {
    return '今日暂无预约';
  }
  return `${dayjs(clickedDate.value).format('YYYY年MM月DD日')} 暂无预约`;
};

const goBack = () => {
  router.back();
};

const getAppointmentsForDate = (date: string) => {
  return appointments.value.filter(apt => {
    const appointmentDate = dayjs(apt.appointmentTime).format('YYYY-MM-DD');
    return appointmentDate === date;
  });
};

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '待确认',
    1: '已确认',
    2: '服务中',
    3: '已完成',
    4: '已取消'
  };
  return statusMap[status] || '未知';
};

const getStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    0: 'pending',
    1: 'confirmed',
    2: 'in_progress',
    3: 'completed',
    4: 'cancelled'
  };
  return classMap[status] || '';
};

const viewAppointment = (id: number) => {
  router.push(`/order/detail/${id}`);
};

// 加载日程数据
const loadSchedule = async () => {
  loading.value = true;
  try {
    // 获取分配给当前服务人员的所有订单
    const res = await orderApi.getOrderList({
      serviceProviderId: userStore.userId,
      pageNum: 1,
      pageSize: 1000, // 获取足够多的订单用于日程显示
    });
    
    if (res && res.code === 200 && res.data) {
      const orders = res.data.records || [];
      
      // 转换订单数据为日程格式
      appointments.value = orders
        .filter((order: any) => {
          // 只显示已确认、服务中、已完成的订单，排除已取消的
          return order.status !== 4 && order.appointmentTime;
        })
        .map((order: any) => {
          const appointmentTime = dayjs(order.appointmentTime);
          return {
            id: order.id,
            orderNo: order.orderNo,
            petName: order.petName,
            petType: order.petType,
            serviceType: order.serviceTypeName || order.servicePackage || '未知服务',
            appointmentTime: order.appointmentTime,
            startTime: appointmentTime.format('HH:mm'),
            date: appointmentTime.format('YYYY-MM-DD'),
            status: order.status,
            contactName: order.contactName,
            contactPhone: order.contactPhone,
            address: order.address,
            duration: 60, // 默认60分钟，可以从服务类型获取
          };
        });
      
      console.log('加载的日程数据:', appointments.value);
    } else {
      appointments.value = [];
    }
  } catch (error) {
    console.error('加载日程数据失败:', error);
    ElMessage.error('加载日程数据失败');
    appointments.value = [];
  } finally {
    loading.value = false;
  }
};

// 监听订单分配和状态更新事件
const handleOrderAssigned = () => {
  loadSchedule();
};

const handleOrderStatusUpdated = () => {
  loadSchedule();
};

onMounted(() => {
  loadSchedule();
  
  // 监听订单分配和状态更新事件
  window.addEventListener('order-assigned', handleOrderAssigned);
  window.addEventListener('order-status-updated', handleOrderStatusUpdated);
});

onUnmounted(() => {
  window.removeEventListener('order-assigned', handleOrderAssigned);
  window.removeEventListener('order-status-updated', handleOrderStatusUpdated);
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

.schedule-container {
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
    margin-top: 24px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }
  }

  :deep(.el-calendar) {
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    overflow: hidden;

    .el-calendar__header {
      background: $bg-light;
      padding: 16px 20px;
      border-bottom: 1px solid $border-color;
    }

    .el-calendar__body {
      padding: 12px;
    }

    .el-calendar-table {
      .el-calendar-day {
        padding: 0;
      }
    }
  }
  
  .calendar-day {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 8px 4px;
    cursor: pointer;
    border-radius: 8px;
    transition: all 0.3s ease;
    min-height: 60px;
    justify-content: center;
    
    &:hover {
      background-color: rgba(64, 158, 255, 0.1);
      transform: scale(1.05);
    }
    
    &.has-appointments {
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(103, 194, 58, 0.05) 100%);
      
      .day-number {
        color: $primary-color;
        font-weight: 700;
        font-size: 16px;
      }
    }
    
    &.selected-date {
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.2) 0%, rgba(102, 126, 234, 0.15) 100%);
      border: 2px solid $primary-color;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
      
      .day-number {
        color: $primary-color;
        font-weight: 700;
        font-size: 18px;
      }
    }
    
    .day-number {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
      margin-bottom: 4px;
    }
    
    .appointments-count {
      font-size: 11px;
      color: $primary-color;
      margin-top: 4px;
      font-weight: 600;
      background: rgba(64, 158, 255, 0.1);
      padding: 2px 6px;
      border-radius: 999px;
    }
  }
  
  .appointments-list {
    background: $bg-white;
    border-radius: 12px;
    border: 1px solid $border-color;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    padding: 24px;
    
    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      padding-bottom: 16px;
      border-bottom: 2px solid $border-color;
      
      h3 {
        margin: 0;
        font-size: 20px;
        color: $text-primary;
        font-weight: 700;
      }

      :deep(.el-button) {
        border-radius: 8px;
        font-weight: 500;
      }
    }
    
    .appointments {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    
    .appointment-item {
      background: $bg-white;
      padding: 20px;
      border-radius: 12px;
      border: 1px solid $border-color;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      display: flex;
      gap: 20px;
      cursor: pointer;
      transition: all 0.3s ease;
      
      &:hover {
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
        transform: translateY(-2px);
        border-color: $primary-color;
      }
      
      .appointment-time {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-width: 100px;
        padding: 12px;
        background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(102, 126, 234, 0.08) 100%);
        border-radius: 10px;
        border-left: 3px solid $primary-color;
        
        .time {
          font-size: 20px;
          font-weight: 700;
          color: $primary-color;
        }
        
        .duration {
          font-size: 12px;
          color: $text-light;
          margin-top: 6px;
          font-weight: 500;
        }
      }
      
      .appointment-info {
        flex: 1;
        
        h4 {
          margin: 0 0 10px;
          font-size: 18px;
          color: $text-primary;
          font-weight: 600;
        }
        
        p {
          margin: 0 0 10px;
          font-size: 14px;
          color: $text-secondary;
          line-height: 1.6;
        }
        
        .pet-type {
          font-size: 14px;
          color: $text-light;
          font-weight: normal;
        }
        
        .contact-info {
          font-size: 13px;
          color: $text-secondary;
          margin: 8px 0;
          display: flex;
          gap: 16px;
          flex-wrap: wrap;
        }
        
        .address {
          font-size: 13px;
          color: $text-light;
          margin: 8px 0;
          padding: 8px 12px;
          background: $bg-light;
          border-radius: 6px;
          border-left: 3px solid $primary-color;
        }
        
        .status {
          display: inline-block;
          padding: 4px 12px;
          border-radius: 999px;
          font-size: 12px;
          font-weight: 500;
          margin-top: 12px;
          
          &.pending {
            background: rgba(230, 162, 60, 0.1);
            color: $warning-color;
            border: 1px solid rgba(230, 162, 60, 0.3);
          }
          
          &.confirmed {
            background: rgba(64, 158, 255, 0.1);
            color: $primary-color;
            border: 1px solid rgba(64, 158, 255, 0.3);
          }
          
          &.in_progress {
            background: rgba(103, 194, 58, 0.1);
            color: $success-color;
            border: 1px solid rgba(103, 194, 58, 0.3);
          }
          
          &.completed {
            background: rgba(103, 194, 58, 0.1);
            color: $success-color;
            border: 1px solid rgba(103, 194, 58, 0.3);
          }
          
          &.cancelled {
            background: rgba(245, 108, 108, 0.1);
            color: $danger-color;
            border: 1px solid rgba(245, 108, 108, 0.3);
          }
        }
      }
    }
  }
}
</style>


