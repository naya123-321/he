import request from "./request";

// 创建反馈
export const createFeedback = (data: FeedbackCreateDTO) => {
  return request.post<ApiResponse<FeedbackVO>>("/feedback/create", data);
};

// 获取服务人员的反馈列表
export const getProviderFeedbackList = (params?: {
  status?: number;
  pageNum?: number;
  pageSize?: number;
}) => {
  return request.get<ApiResponse<PageResult<FeedbackVO>>>("/feedback/provider/list", {
    params,
  });
};

// 获取用户的反馈列表
export const getUserFeedbackList = (params?: {
  pageNum?: number;
  pageSize?: number;
}) => {
  return request.get<ApiResponse<PageResult<FeedbackVO>>>("/feedback/user/list", {
    params,
  });
};

// 处理反馈（服务人员回复）
export const respondToFeedback = (data: FeedbackResponseDTO) => {
  return request.post<ApiResponse<FeedbackVO>>("/feedback/respond", data);
};

// 创建反馈（服务人员主动提交给宠主）
export const createFeedbackByProvider = (data: FeedbackCreateDTO) => {
  return request.post<ApiResponse<FeedbackVO>>("/feedback/provider/create", data);
};

// 宠主回复服务人员主动发送的反馈
export const respondToProviderFeedback = (data: FeedbackResponseDTO) => {
  return request.post<ApiResponse<FeedbackVO>>("/feedback/user/respond", data);
};

// 获取服务人员主动发送的反馈列表
export const getProviderSentFeedbackList = (params?: {
  pageNum?: number;
  pageSize?: number;
}) => {
  return request.get<ApiResponse<PageResult<FeedbackVO>>>("/feedback/provider/sent", {
    params,
  });
};

// 定义接口类型
export interface FeedbackCreateDTO {
  orderId: number;
  content: string;
}

export interface FeedbackResponseDTO {
  feedbackId: number;
  response: string;
}

export interface FeedbackVO {
  id?: number;
  orderId: number;
  orderNo?: string;
  userId?: number;
  username?: string;
  petName?: string;
  serviceProviderId?: number;
  serviceProviderName?: string;
  content: string;
  response?: string;
  status?: number;
  statusText?: string;
  createTime?: string;
  updateTime?: string;
  responseTime?: string;
  isFromProvider?: boolean; // 是否是服务人员主动发送的反馈
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

