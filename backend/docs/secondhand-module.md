## 二手模块实现说明

### 模块范围

本次实现提供了二手交易的基础闭环能力：

- 二手商品公共列表与详情；
- 用户发布、编辑、删除、上下架自己的二手商品；
- 买家购买二手商品并自动创建订单（`order_info` + `order_item`）。

### 主要后端接口

- `GET /api/secondhand/list`：分页查询在售二手商品（支持关键字、价格区间、成色筛选）
- `GET /api/secondhand/detail/{productId}`：获取二手商品详情
- `GET /api/secondhand/seller/list`：查询我发布的二手商品
- `POST /api/secondhand/seller`：发布二手商品
- `PUT /api/secondhand/seller/{productId}`：编辑二手商品
- `DELETE /api/secondhand/seller/{productId}`：删除二手商品
- `POST /api/secondhand/seller/{productId}/status`：切换上下架
- `POST /api/secondhand/{productId}/buy`：购买二手商品并创建订单

### 关键业务规则

- 二手商品状态：`1=在售`，`2=下架`。
- 用户只能编辑/删除/上下架自己发布的二手商品。
- 购买时会做并发保护：仅对状态为在售的商品执行原子更新，防止重复售卖。
- 用户不能购买自己发布的二手商品。
- 购买成功后自动生成订单，订单项 `product_type` 设为 `SECONDHAND`。

### OpenAPI 请求/响应示例

#### 1) 发布二手商品

请求：

```http
POST /api/secondhand/seller
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "闲置耳机",
  "cover": "http://localhost:8080/static/upload/used-headphone.png",
  "description": "成色较新，功能正常",
  "originPrice": 399.00,
  "salePrice": 180.00,
  "conditionLevel": "90%",
  "status": 1
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10,
    "sellerUserId": 3,
    "name": "闲置耳机",
    "cover": "http://localhost:8080/static/upload/used-headphone.png",
    "description": "成色较新，功能正常",
    "originPrice": 399.00,
    "salePrice": 180.00,
    "conditionLevel": "90%",
    "status": 1,
    "statusName": "在售",
    "createTime": "2026-04-07T20:00:00"
  }
}
```

#### 2) 购买二手商品

请求：

```http
POST /api/secondhand/2/buy
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "remark": "请尽快发货"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 20,
    "orderNo": "SND202604072000000003",
    "totalAmount": 180.00,
    "payStatus": 1,
    "orderStatus": 1,
    "remark": "请尽快发货",
    "createTime": "2026-04-07T20:00:00",
    "items": [
      {
        "id": 30,
        "productId": 2,
        "productName": "Spare Headphones",
        "price": 180.00,
        "quantity": 1
      }
    ]
  }
}
```

