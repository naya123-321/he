# 前后端联调测试说明

## 已完成的功能

### 1. 后端基础架构
- ✅ 统一响应格式（Result类）
- ✅ 分页结果类（PageResult）
- ✅ CORS跨域配置
- ✅ 全局异常处理

### 2. 用户认证模块
- ✅ 用户注册 (`POST /api/auth/register`)
- ✅ 用户登录 (`POST /api/auth/login`)
- ✅ 检查用户名 (`GET /api/auth/check-username`)
- ✅ 获取用户信息 (`GET /api/auth/user-info`)
- ✅ 验证用户身份 (`POST /api/auth/verify-identity`)
- ✅ 重置密码 (`POST /api/auth/reset-password`)
- ✅ 发送验证码（模拟）(`POST /api/auth/send-verification-code`)
- ✅ 验证邮箱验证码（模拟）(`POST /api/auth/verify-code`)

### 3. 订单管理模块
- ✅ 创建订单 (`POST /api/order/create`)
- ✅ 获取订单列表 (`GET /api/order/list`)
- ✅ 获取订单详情 (`GET /api/order/detail/{id}`)
- ✅ 取消订单 (`POST /api/order/cancel/{id}`)
- ✅ 更新订单状态 (`POST /api/order/update-status`)
- ✅ 完成订单并评价 (`POST /api/order/complete/{id}`)
- ✅ 添加服务记录 (`POST /api/order/add-note/{id}`)

### 4. 纪念册模块
- ✅ 创建纪念册 (`POST /api/memorial/create`)
- ✅ 保存内容 (`POST /api/memorial/save-content`)
- ✅ 自动保存 (`POST /api/memorial/auto-save/{id}`)
- ✅ 发布纪念册 (`POST /api/memorial/publish`)
- ✅ 获取纪念册列表 (`GET /api/memorial/list`)
- ✅ 获取纪念册详情 (`GET /api/memorial/detail/{id}`)
- ✅ 点赞 (`POST /api/memorial/like/{id}`)
- ✅ 取消点赞 (`POST /api/memorial/unlike/{id}`)
- ✅ 收藏（模拟）(`POST /api/memorial/favorite/{id}`)

### 5. 服务类型模块
- ✅ 获取服务类型列表 (`GET /api/service-type/list`)

## 数据库表结构

应用启动后，Hibernate会自动创建以下表：
- `users` - 用户表
- `orders` - 订单表
- `memorials` - 纪念册表
- `service_types` - 服务类型表

## 启动步骤

### 1. 启动后端
```bash
cd backend/demo
.\mvnw.cmd spring-boot:run
```

后端将在 `http://localhost:8080` 启动

### 2. 启动前端
```bash
cd frontend
npm run dev
```

前端将在 `http://localhost:8081` 启动，并自动代理API请求到后端

## 测试流程

### 1. 用户注册和登录
1. 访问 `http://localhost:8081/register`
2. 注册一个新用户（角色：0-宠主，1-服务人员，2-管理员）
3. 登录获取token

### 2. 创建服务类型（可选）
如果需要测试订单功能，可以先在数据库中插入服务类型数据：
```sql
INSERT INTO service_types (name, description, price, duration, status) 
VALUES ('基础服务', '基础宠物殡葬服务', 500.00, 60, 1);
```

### 3. 创建订单
1. 登录后访问订单创建页面
2. 填写订单信息并提交
3. 查看订单列表和详情

### 4. 创建纪念册
1. 登录后访问纪念册创建页面
2. 创建纪念册并保存内容
3. 发布纪念册

## Token说明

当前实现使用简单的token格式：`token_{userId}_{timestamp}`

实际生产环境应使用JWT token，当前实现仅用于联调测试。

## 注意事项

1. **密码加密**：使用BCrypt加密，注册和登录时密码会自动加密/验证
2. **时间格式**：使用 `yyyy-MM-dd HH:mm:ss` 格式
3. **分页参数**：pageNum从1开始，pageSize默认为10
4. **角色说明**：
   - 0: 宠主
   - 1: 服务人员
   - 2: 管理员

## API响应格式

所有API响应格式统一为：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1234567890
}
```

错误响应：
```json
{
  "code": 500,
  "message": "错误信息",
  "data": null,
  "timestamp": 1234567890
}
```

## 常见问题

1. **跨域问题**：已配置CORS，应该不会有跨域问题
2. **数据库连接失败**：检查MySQL是否启动，数据库是否创建
3. **Token无效**：确保请求头中包含 `Authorization: Bearer {token}`















