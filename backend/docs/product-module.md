## 商品模块实现与接口说明

### 一、模块职责概述

- **面向用户（买家/游客）**：提供在售商品的分页列表和详情查看能力，支持关键词和价格区间筛选。
- **面向卖家**：提供商品的新增、编辑、删除、上下架和库存调整能力，所有操作均基于当前登录卖家的店铺进行权限校验。

后端主要涉及的类包括：

- 控制层：`ProductController`
- 业务层：`ProductService` / `ProductServiceImpl`
- 持久层：`ProductMapper`、实体 `Product`
- 传输对象：`ProductPageQueryRequest`、`ProductSaveRequest`、`ProductStatusUpdateRequest`、`ProductStockAdjustRequest`、`ProductVO`、`PageVO`

所有接口统一返回 `Result<T>` 结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 二、公共商品接口（游客可访问）

#### 1. 分页查询在售商品

- **接口地址**：`GET /api/product/list`
- **说明**：查询在售商品，按照创建时间倒序，支持关键词和价格区间筛选。

**请求查询参数示例：**

```http
GET /api/product/list?pageNum=1&pageSize=10&keyword=手机&minPrice=1000&maxPrice=3000
```

等价 JSON 表示（前端一般以 query string 形式传递）：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "keyword": "手机",
  "minPrice": 1000,
  "maxPrice": 3000
}
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 2,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "shopId": 1,
        "name": "旗舰智能手机",
        "cover": "http://localhost:8080/static/upload/product-1.png",
        "description": "高性能旗舰手机，支持5G网络",
        "price": 2599.00,
        "stock": 100,
        "status": 1,
        "statusName": "上架",
        "createTime": "2024-01-01T10:00:00"
      },
      {
        "id": 2,
        "shopId": 1,
        "name": "学生笔记本电脑",
        "cover": "http://localhost:8080/static/upload/product-2.png",
        "description": "轻薄便携，适合日常学习办公",
        "price": 3999.00,
        "stock": 50,
        "status": 1,
        "statusName": "上架",
        "createTime": "2024-01-02T09:30:00"
      }
    ]
  }
}
```

#### 2. 获取商品详情

- **接口地址**：`GET /api/product/detail/{productId}`
- **说明**：仅返回处于“上架”状态的商品；若商品不存在或已下架，返回 404 业务码。

**请求示例：**

```http
GET /api/product/detail/1
```

**成功响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "shopId": 1,
    "name": "旗舰智能手机",
    "cover": "http://localhost:8080/static/upload/product-1.png",
    "description": "高性能旗舰手机，支持5G网络",
    "price": 2599.00,
    "stock": 100,
    "status": 1,
    "statusName": "上架",
    "createTime": "2024-01-01T10:00:00"
  }
}
```

**商品不存在或已下架时的响应示例：**

```json
{
  "code": 404,
  "message": "商品不存在或已下架",
  "data": null
}
```

### 三、卖家商品管理接口（需登录，卖家角色）

所有 `/api/product/seller/**` 接口均要求：

- 前端在请求头中携带 `Authorization: Bearer <token>`；
- 当前登录用户角色为 `OFFICIAL_SELLER` 或 `SELLER`；
- 系统会自动根据当前用户找到其名下的有效店铺，仅允许操作自己店铺下的商品。

#### 1. 卖家分页查询商品

- **接口地址**：`GET /api/product/seller/list`

**请求参数示例：**

```http
GET /api/product/seller/list?pageNum=1&pageSize=10&status=1&keyword=手机
Authorization: Bearer <token>
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "shopId": 1,
        "name": "旗舰智能手机",
        "cover": "http://localhost:8080/static/upload/product-1.png",
        "description": "高性能旗舰手机，支持5G网络",
        "price": 2599.00,
        "stock": 100,
        "status": 1,
        "statusName": "上架",
        "createTime": "2024-01-01T10:00:00"
      }
    ]
  }
}
```

#### 2. 卖家新增商品

- **接口地址**：`POST /api/product/seller`
- **请求体说明**：对应 `ProductSaveRequest`。

**请求 JSON 示例：**

```json
{
  "name": "新款蓝牙耳机",
  "cover": "http://localhost:8080/static/upload/earphone.png",
  "description": "降噪入耳式蓝牙耳机",
  "price": 199.00,
  "stock": 200,
  "status": 1
}
```

**成功响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 5,
    "shopId": 1,
    "name": "新款蓝牙耳机",
    "cover": "http://localhost:8080/static/upload/earphone.png",
    "description": "降噪入耳式蓝牙耳机",
    "price": 199.00,
    "stock": 200,
    "status": 1,
    "statusName": "上架",
    "createTime": "2024-01-10T14:30:00"
  }
}
```

#### 3. 卖家更新商品

- **接口地址**：`PUT /api/product/seller/{productId}`

**请求 JSON 示例：**

```json
{
  "name": "新款蓝牙耳机（升级版）",
  "cover": "http://localhost:8080/static/upload/earphone-v2.png",
  "description": "支持双设备连接的升级版蓝牙耳机",
  "price": 249.00,
  "stock": 180,
  "status": 1
}
```

**成功响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 5,
    "shopId": 1,
    "name": "新款蓝牙耳机（升级版）",
    "cover": "http://localhost:8080/static/upload/earphone-v2.png",
    "description": "支持双设备连接的升级版蓝牙耳机",
    "price": 249.00,
    "stock": 180,
    "status": 1,
    "statusName": "上架",
    "createTime": "2024-01-10T14:30:00"
  }
}
```

若当前登录卖家试图操作他人店铺商品，后端会返回 403 业务码：

```json
{
  "code": 403,
  "message": "无权操作该商品",
  "data": null
}
```

#### 4. 卖家删除商品

- **接口地址**：`DELETE /api/product/seller/{productId}`

**成功响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 5. 卖家切换商品上下架状态

- **接口地址**：`POST /api/product/seller/{productId}/status`
- **请求体说明**：对应 `ProductStatusUpdateRequest`，仅包含 `status` 字段。

**请求 JSON 示例：**

```json
{
  "status": 2
}
```

> 其中 `status` 的取值与 `ProductStatusEnum` 对应，例如：`1=ON_SHELF(上架)，2=OFF_SHELF(下架)`。

**成功响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 5,
    "shopId": 1,
    "name": "新款蓝牙耳机",
    "cover": "http://localhost:8080/static/upload/earphone.png",
    "description": "降噪入耳式蓝牙耳机",
    "price": 199.00,
    "stock": 200,
    "status": 2,
    "statusName": "下架",
    "createTime": "2024-01-10T14:30:00"
  }
}
```

#### 6. 卖家调整商品库存

- **接口地址**：`POST /api/product/seller/{productId}/stock/adjust`
- **请求体说明**：对应 `ProductStockAdjustRequest`，包含 `delta` 字段（可以为正负整数）。

**请求 JSON 示例：**

```json
{
  "delta": -5
}
```

- 若调整后库存为负数，接口返回 400 业务码；
- 若 `delta` 为 0，则返回 400 业务码并提示“库存变更值不能为0”。

**成功响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 5,
    "shopId": 1,
    "name": "新款蓝牙耳机",
    "cover": "http://localhost:8080/static/upload/earphone.png",
    "description": "降噪入耳式蓝牙耳机",
    "price": 199.00,
    "stock": 195,
    "status": 1,
    "statusName": "上架",
    "createTime": "2024-01-10T14:30:00"
  }
}
```

### 四、本模块在文档与答辩中的说明建议

- 在课程报告中，可将本模块归入“商品与搜索模块”章节，重点说明：
  - 如何通过公共接口实现“游客浏览商品列表与详情”；
  - 如何通过卖家接口实现“店铺维度的商品管理、上下架与库存维护”；
  - 价格区间校验、非法状态校验、权限校验（仅能操作自己店铺商品）等关键业务规则。
- 在答辩 PR 中可直接引用本文件路径，并附上几条核心接口的 Postman / Apifox 截图作为演示材料。

