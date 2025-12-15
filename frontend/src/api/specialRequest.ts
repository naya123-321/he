import request from "./request";

export interface SpecialRequestCreateDTO {
  orderId: number;
  requestType?: string;
  description: string;
}

export interface SpecialRequestReviewDTO {
  requestId: number;
  status: number; // 1:已通过 2:已拒绝
  adminComment?: string;
}

export interface SpecialRequestVO {
  id: number;
  orderId: number;
  orderNo: string;
  userId: number;
  userName: string;
  userPhone: string;
  requestType: string;
  description: string;
  status: number;
  statusText: string;
  adminComment?: string;
  adminId?: number;
  adminName?: string;
  reviewTime?: string;
  createTime: string;
  updateTime?: string;
}

export interface ApiResponse<T> {
  code: number;
  data: T;
  message: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export const specialRequestApi = {
  // 创建特殊需求
  createSpecialRequest: (data: SpecialRequestCreateDTO) => {
    return request.post<ApiResponse<SpecialRequestVO>>("/special-request/create", data);
  },

  // 获取特殊需求列表（管理员）
  getSpecialRequestList: (params?: { status?: number; pageNum?: number; pageSize?: number }) => {
    return request.get<ApiResponse<PageResult<SpecialRequestVO>>>("/special-request/list", {
      params,
    });
  },

  // 获取用户的特殊需求列表
  getUserSpecialRequestList: (params?: { pageNum?: number; pageSize?: number }) => {
    return request.get<ApiResponse<PageResult<SpecialRequestVO>>>("/special-request/user/list", {
      params,
    });
  },

  // 获取特殊需求详情
  getSpecialRequestDetail: (id: number) => {
    return request.get<ApiResponse<SpecialRequestVO>>(`/special-request/detail/${id}`);
  },

  // 审核特殊需求
  reviewSpecialRequest: (data: SpecialRequestReviewDTO) => {
    return request.post<ApiResponse<SpecialRequestVO>>("/special-request/review", data);
  },

  // 根据订单ID获取已通过审核的特殊需求
  getApprovedSpecialRequestByOrderId: (orderId: number) => {
    return request.get<ApiResponse<SpecialRequestVO | null>>(`/special-request/by-order/${orderId}`);
  },
};
