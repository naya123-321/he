import request from "./request";

export interface UrnStorageCreateDTO {
  orderId: number;
  petName: string;
  petType?: string;
  urnNo: string;
  storageDate: string; // YYYY-MM-DD
  storagePeriod: number; // 月
  storageLocation?: string;
  remark?: string;
}

export interface UrnStorageVO {
  id: number;
  orderId: number;
  orderNo?: string;
  userId: number;
  userName?: string;
  userPhone?: string;
  petName: string;
  petType?: string;
  urnNo: string;
  storageDate: string;
  expiryDate: string;
  storagePeriod: number;
  storageLocation?: string;
  status: number;
  statusText: string;
  remark?: string;
  createTime: string;
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
  data: T;
  message: string;
}

export const urnStorageApi = {
  // 创建骨灰寄存记录
  createUrnStorage: (data: UrnStorageCreateDTO) => {
    return request.post<ApiResponse<UrnStorageVO>>("/urn-storage/create", data);
  },

  // 获取骨灰寄存列表
  getUrnStorageList: (params?: {
    userId?: number;
    status?: number;
    petName?: string;
    urnNo?: string;
    orderId?: number; // 添加 orderId 参数
    pageNum?: number;
    pageSize?: number;
  }) => {
    return request.get<ApiResponse<PageResult<UrnStorageVO>>>("/urn-storage/list", {
      params,
    });
  },

  // 获取骨灰寄存详情
  getUrnStorageDetail: (id: number) => {
    return request.get<ApiResponse<UrnStorageVO>>(`/urn-storage/detail/${id}`);
  },

  // 取回骨灰
  retrieveUrnStorage: (id: number) => {
    return request.post<ApiResponse<UrnStorageVO>>(`/urn-storage/retrieve/${id}`);
  },

  // 更新骨灰寄存信息
  updateUrnStorage: (id: number, data: UrnStorageCreateDTO) => {
    return request.put<ApiResponse<UrnStorageVO>>(`/urn-storage/${id}`, data);
  },

  // 删除骨灰寄存记录
  deleteUrnStorage: (id: number) => {
    return request.delete<ApiResponse<boolean>>(`/urn-storage/${id}`);
  },

  // 审批骨灰寄存请求
  approveUrnStorageRequest: (id: number, approved: boolean) => {
    return request.post<ApiResponse<UrnStorageVO>>(`/urn-storage/approve/${id}?approved=${approved}`);
  },
};

