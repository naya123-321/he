import request from "./request";

// 订单相关接口
export const orderApi = {
  // 创建订单
  createOrder: (data: OrderCreateDTO) => {
    return request.post<ApiResponse<OrderVO>>("/order/create", data);
  },

  // 取消订单
  cancelOrder: (id: number, reason: string) => {
    return request.post<ApiResponse<boolean>>(
      `/order/cancel/${id}?reason=${reason}`
    );
  },

  // 查询订单列表
  getOrderList: (params: OrderQueryDTO) => {
    return request.get<ApiResponse<PageResult<OrderVO>>>("/order/list", {
      params,
    });
  },

  // 获取订单详情
  getOrderDetail: (id: number) => {
    return request.get<ApiResponse<OrderVO>>(`/order/detail/${id}`);
  },

  // 更新订单状态
  updateOrderStatus: (data: OrderStatusUpdateDTO) => {
    return request.post<ApiResponse<boolean>>("/order/update-status", data);
  },

  // 分配服务人员
  assignServiceProvider: (data: { orderId: number; serviceProviderId: number }) => {
    return request.post<ApiResponse<boolean>>("/order/assign-provider", data);
  },

  // 添加服务记录
  addServiceNote: (id: number, notes: string) => {
    return request.post<ApiResponse<boolean>>(
      `/order/add-note/${id}?notes=${notes}`
    );
  },

  // 完成订单并评价
  completeOrder: (id: number, rating?: number, review?: string) => {
    return request.post<ApiResponse<boolean>>(
      `/order/complete/${id}?rating=${rating || ""}&review=${review || ""}`
    );
  },

  // 宠主提交满意度评价（不改变订单状态，仅已完成订单）
  submitReview: (id: number, rating: number, review?: string) => {
    return request.post<ApiResponse<boolean>>(
      `/order/review/${id}?rating=${rating}&review=${encodeURIComponent(review || "")}`
    );
  },

  // 获取订单状态日志
  getStatusLogs: (id: number) => {
    return request.get<ApiResponse<any[]>>(`/order/status-logs/${id}`);
  },

  // 获取满意度统计数据
  getSatisfactionStats: () => {
    return request.get<ApiResponse<{
      averageSatisfaction: number;
      totalReviews: number;
      fiveStarReviews: number;
      fourStarReviews: number;
      threeStarReviews: number;
      twoStarReviews: number;
      oneStarReviews: number;
    }>>("/order/satisfaction-stats");
  },

  // 获取满意度趋势数据
  getSatisfactionTrend: (days?: number) => {
    return request.get<ApiResponse<{
      dateLabels: string[];
      averageRatings: number[];
      reviewCounts: number[];
    }>>("/order/satisfaction-trend", {
      params: { days },
    });
  },

  // 获取服务类型分布数据
  getServiceTypeDistribution: () => {
    return request.get<ApiResponse<{
      distribution: Array<{
        serviceTypeId: number;
        serviceTypeName: string;
        count: number;
      }>;
      totalOrders: number;
    }>>("/order/service-type-distribution");
  },

  // 获取订单趋势数据
  getOrderTrend: (days?: number) => {
    return request.get<ApiResponse<{
      dateLabels: string[];
      orderCounts: number[];
      totalOrders: number;
    }>>("/order/order-trend", {
      params: { days },
    });
  },
};

// 服务类型接口
export const serviceTypeApi = {
  // 获取所有服务类型（管理员可查看所有，普通用户只能查看启用的）
  getAll: () => {
    return request.get<ApiResponse<ServiceTypeVO[]>>("/service-type/list");
  },

  // 为了兼容性，添加getServiceTypes方法
  getServiceTypes: () => {
    return request.get<ApiResponse<ServiceTypeVO[]>>("/service-type/list");
  },

  // 获取启用的服务类型（公开接口）
  getEnabled: () => {
    return request.get<ApiResponse<ServiceTypeVO[]>>("/service-type/enabled");
  },

  // 根据ID获取服务类型
  getById: (id: number) => {
    return request.get<ApiResponse<ServiceTypeVO>>(`/service-type/${id}`);
  },

  // 创建服务类型（仅管理员）
  create: (data: ServiceTypeVO) => {
    return request.post<ApiResponse<ServiceTypeVO>>("/service-type/create", data);
  },

  // 更新服务类型（仅管理员）
  update: (id: number, data: ServiceTypeVO) => {
    return request.put<ApiResponse<ServiceTypeVO>>(`/service-type/${id}`, data);
  },

  // 删除服务类型（仅管理员）
  delete: (id: number) => {
    return request.delete<ApiResponse<boolean>>(`/service-type/${id}`);
  },

  // 更新服务类型状态（启用/禁用，仅管理员）
  updateStatus: (id: number, status: number) => {
    return request.put<ApiResponse<ServiceTypeVO>>(`/service-type/${id}/status`, null, {
      params: { status },
    });
  },
};

// 类型定义
export interface OrderCreateDTO {
  serviceTypeId: number;
  petName: string;
  petType: string;
  petBreed?: string;
  petAge?: number;
  petWeight?: number;
  deathDate: string;
  deathCause?: string;
  appointmentTime: string;
  address: string;
  contactName: string;
  contactPhone: string;
  specialRequirements?: string;
}

export interface OrderQueryDTO {
  userId?: number;
  serviceProviderId?: number;
  status?: number;
  petName?: string;
  orderNo?: string;
  startDate?: string;
  endDate?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface OrderStatusUpdateDTO {
  orderId: number;
  targetStatus: number;
  remark?: string;
}

export interface OrderVO {
  id: number;
  orderNo: string;
  userId: number;
  username: string;
  avatar?: string;

  serviceTypeId: number;
  serviceTypeName: string;
  servicePrice: number;

  petName: string;
  petType: string;
  petBreed?: string;
  petAge?: number;
  petWeight?: number;
  deathDate: string;
  deathCause?: string;

  appointmentTime: string;
  address: string;
  contactName: string;
  contactPhone: string;
  specialRequirements?: string;

  status: number;
  statusText: string;

  totalAmount: number;
  actualAmount: number;
  paymentStatus: number;
  paymentStatusText: string;

  serviceProviderId?: number;
  serviceProviderName?: string;
  serviceProviderPhone?: string;
  serviceNotes?: string;
  completionTime?: string;

  rating?: number;
  review?: string;

  createTime: string;
}

export interface ServiceTypeVO {
  id?: number;
  name: string;
  description?: string;
  price: number;
  duration?: number;
  process?: string;
  status?: number;
  isRecommended?: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 订单查询参数类型
export interface OrderQueryParams {
  status?: number;
  petName?: string;
  orderNo?: string;
  startTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}

// 订单统计类型
export interface OrderStat {
  status?: number;
  label: string;
  count: number;
  className: string;
}
