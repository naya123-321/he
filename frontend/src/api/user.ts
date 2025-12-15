import request from "./request";

// 用户登录
export const login = (data: LoginDTO) => {
  return request.post<ApiResponse<string>>("/auth/login", data);
};

// 用户注册
export const register = (data: RegisterDTO) => {
  return request.post<ApiResponse<boolean>>("/auth/register", data);
};

// 检查用户名是否存在
export const checkUsername = (username: string) => {
  return request.get<ApiResponse<boolean>>(
    `/auth/check-username?username=${username}`
  );
};

// 获取用户信息
export const getUserInfo = () => {
  return request.get<ApiResponse<UserVO>>("/auth/user-info");
};

// 发送验证码
export const sendVerificationCode = (email: string) => {
  return request.post<ApiResponse<boolean>>("/auth/send-verification-code", {
    email,
  });
};

// 验证用户身份
export const verifyUserIdentity = (username: string, email: string) => {
  return request.post<ApiResponse<boolean>>("/auth/verify-identity", {
    username,
    email,
  });
};

// 验证邮箱验证码
export const verifyEmailCode = (email: string, code: string) => {
  return request.post<ApiResponse<boolean>>("/auth/verify-code", {
    email,
    code,
  });
};

// 重置密码
export const resetPassword = (username: string, newPassword: string) => {
  return request.post<ApiResponse<boolean>>("/auth/reset-password", {
    username,
    newPassword,
  });
};

// 更新用户信息
export const updateUserInfo = (data: UpdateUserInfoDTO) => {
  return request.put<ApiResponse<UserVO>>("/auth/user/update-info", data);
};

// 修改密码
export const changePassword = (data: ChangePasswordDTO) => {
  return request.put<ApiResponse<boolean>>("/user/change-password", data);
};

// 获取用户列表（管理员功能）
export const getUserList = (params?: {
  role?: number;
  keyword?: string;
  pageNum?: number;
  pageSize?: number;
}) => {
  return request.get<ApiResponse<PageResult<UserVO>>>("/auth/user-list", {
    params,
  });
};

// 更新用户信息（管理员功能）
export const updateUser = (userId: number, data: UpdateUserDTO) => {
  return request.put<ApiResponse<boolean>>(`/auth/user/${userId}`, data);
};

// 删除用户（管理员功能）
export const deleteUser = (userId: number) => {
  return request.delete<ApiResponse<boolean>>(`/auth/user/${userId}`);
};

// 管理员重置用户密码
export const adminResetPassword = (userId: number, newPassword: string) => {
  return request.post<ApiResponse<boolean>>(`/auth/user/${userId}/reset-password`, {
    newPassword,
  });
};

// 定义接口类型
export interface LoginDTO {
  username: string;
  password: string;
  role?: number;
}

export interface RegisterDTO {
  username: string;
  password: string;
  email?: string;
  phone?: string;
  role?: number;
}

export interface UpdateUserInfoDTO {
  nickname?: string;
  email?: string;
  phone?: string;
  avatar?: string;
  gender?: string;
  birthday?: string;
  bio?: string;
}

export interface ChangePasswordDTO {
  oldPassword: string;
  newPassword: string;
}

export interface UserVO {
  id?: number;
  username: string;
  nickname?: string;
  email?: string;
  phone?: string;
  avatar?: string;
  role: number;
  roleName?: string;
  gender?: string;
  birthday?: string;
  bio?: string;
  createTime?: string;
  updateTime?: string;
}

export interface UpdateUserDTO {
  nickname?: string;
  email?: string;
  phone?: string;
  role?: number;
  gender?: string;
  birthday?: string;
  bio?: string;
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
