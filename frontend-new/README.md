# frontend-new

这是在不改动现有 frontend 目录的前提下，新建的重构版前端。

## 目标

- 保持后端接口兼容，仅在前端层做重构
- 商品列表采用淘宝/闲鱼风格的矩形卡片展示
- 商品流支持无限滚动（IntersectionObserver）

## 运行

```bash
cd frontend-new
npm install
npm run dev
```

默认地址: <http://localhost:5174>

后端接口默认使用: <http://localhost:8080/api>

如需调整，在 src/api/http.js 中修改 baseURL。
