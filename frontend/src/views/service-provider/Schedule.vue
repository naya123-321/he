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
              <h4>{{ appointment.petName }} <span class="pet-type">({{ appointment.petType }})</span></h4>
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
.schedule-container {
  padding: 20px;
  
  .content-wrapper {
    margin-top: 20px;
  }
  
  .calendar-day {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 4px;
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.2s;
    
    &:hover {
      background-color: #f5f7fa;
    }
    
    &.has-appointments {
      .day-number {
        color: #409eff;
        font-weight: 600;
      }
    }
    
    &.selected-date {
      background-color: #ecf5ff;
      border: 1px solid #409eff;
      
      .day-number {
        color: #409eff;
        font-weight: 600;
      }
    }
    
    .day-number {
      font-size: 14px;
      font-weight: 500;
    }
    
    .appointments-count {
      font-size: 10px;
      color: #409eff;
      margin-top: 4px;
      font-weight: 500;
    }
  }
  
  .appointments-list {
    margin-top: 30px;
    
    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      
      h3 {
        margin: 0;
        font-size: 18px;
        color: #303133;
      }
    }
    
    .appointments {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .appointment-item {
      background: white;
      padding: 16px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      display: flex;
      gap: 16px;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }
      
      .appointment-time {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-width: 80px;
        padding: 8px;
        background: #f5f7fa;
        border-radius: 4px;
        
        .time {
          font-size: 16px;
          font-weight: bold;
          color: #303133;
        }
        
        .duration {
          font-size: 12px;
          color: #909399;
          margin-top: 4px;
        }
      }
      
      .appointment-info {
        flex: 1;
        
        h4 {
          margin: 0 0 8px;
          font-size: 16px;
          color: #303133;
        }
        
        p {
          margin: 0 0 8px;
          font-size: 14px;
          color: #606266;
        }
        
        .pet-type {
          font-size: 14px;
          color: #909399;
          font-weight: normal;
        }
        
        .contact-info {
          font-size: 13px;
          color: #606266;
          margin: 4px 0;
        }
        
        .address {
          font-size: 12px;
          color: #909399;
          margin: 4px 0;
        }
        
        .status {
          display: inline-block;
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          margin-top: 8px;
          
          &.pending {
            background: #fdf6ec;
            color: #e6a23c;
          }
          
          &.confirmed {
            background: #f0f9ff;
            color: #409eff;
          }
          
          &.in_progress {
            background: #f0f9ff;
            color: #67c23a;
          }
          
          &.completed {
            background: #f0f9ff;
            color: #67c23a;
          }
          
          &.cancelled {
            background: #fef0f0;
            color: #f56c6c;
          }
        }
      }
    }
  }
}
</style>


