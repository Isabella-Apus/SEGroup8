# frontend

这是当前使用的前端工程。

## 目标

- 保持后端接口兼容，仅在前端层做重构
- 商品列表采用淘宝/闲鱼风格的矩形卡片展示
- 商品流支持无限滚动（IntersectionObserver）

## 运行

```bash
cd frontend
npm install
npm run dev
```

默认地址: <http://localhost:5174>

后端接口默认使用: <http://localhost:8080/api>

如需调整，在 src/api/http.js 中修改 baseURL。
