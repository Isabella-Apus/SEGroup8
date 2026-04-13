/**
 * 全站「搜索框」行为对照（前端维护说明，非运行时代码）
 *
 * | 页面 | 搜索方式 | 匹配内容 |
 * |------|----------|----------|
 * | OrderView 我的订单 | 请求后端 keyword | 订单号 orderNo 子串 或 订单行商品名 productName 子串（OrderServiceImpl.pageMyOrders） |
 * | ProductListView 商品列表 | 前端 Fuse 模块 | 商品 name、description；中英、拼写容错 |
 * | SecondhandListView 二手列表 | 前端 Fuse 模块 | 同上 |
 * | BrowseHistoryView 浏览记录 | 前端 Fuse 模块 | 商品名 product.name、商品 ID |
 * | AdminUserList 等管理页 | 请求后端 keyword | 见各接口 |
 * | MyReviewView 我的评价 | 请求后端 keyword | 评价内容等（后端分页） |
 *
 * 订单类页面若 placeholder 写「订单号/商品名」，通常表示 keyword 同时覆盖二者（需后端支持）。
 */
export const SEARCH_SCENARIOS_DOC = "see JSDoc above";
