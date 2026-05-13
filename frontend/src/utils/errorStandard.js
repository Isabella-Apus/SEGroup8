const CODE_FALLBACK = {
  400: "请求信息不完整或格式不正确，请检查后重试",
  401: "登录已过期，请重新登录",
  402: "账户余额不足，请充值后重试",
  403: "当前账号没有权限执行此操作",
  404: "请求的数据不存在或已被删除",
  409: "当前操作与已有数据冲突，请刷新后重试",
  410: "该内容已失效，请刷新后重试",
  413: "上传内容过大，请压缩后重试",
  422: "提交内容未通过校验，请检查后重试",
  429: "操作太频繁，请稍后再试",
  500: "服务器处理失败，请稍后重试",
  502: "服务暂时不可用，请稍后重试",
  503: "服务暂时不可用，请稍后重试",
  504: "请求超时，请稍后重试",
};

const FIELD_ALIAS = {
  username: "用户名",
  password: "密码",
  nickname: "昵称",
  phone: "手机号",
  email: "邮箱",
  name: "名称",
  title: "标题",
  description: "描述",
  content: "内容",
  price: "价格",
  originPrice: "原价",
  origin_price: "原价",
  salePrice: "售价",
  sale_price: "售价",
  stock: "库存",
  quantity: "数量",
  amount: "金额",
  cover: "封面图片",
  images: "商品图片",
  categoryId: "一级分类",
  category_id: "一级分类",
  subCategoryId: "二级分类",
  sub_category_id: "二级分类",
  receiverName: "收货人",
  receiver_name: "收货人",
  receiverPhone: "收货手机号",
  receiver_phone: "收货手机号",
  province: "省份",
  city: "城市",
  detailAddress: "详细地址",
  detail_address: "详细地址",
  status: "状态",
};

const KEYWORD_RULES = [
  { keywords: ["stock", "库存"], message: "商品库存不足，请调整数量后重试" },
  { keywords: ["balance", "余额"], message: "账户余额不足，请充值后重试" },
  { keywords: ["token", "jwt", "unauthorized", "登录", "未登录"], code: 401, message: "登录已过期，请重新登录" },
  { keywords: ["forbidden", "permission", "权限", "无权"], code: 403, message: "当前账号没有权限执行此操作" },
  { keywords: ["not found", "不存在", "已删除"], message: "相关数据不存在或已被删除，请刷新后重试" },
  { keywords: ["upload", "file", "文件", "上传"], message: "文件上传失败，请检查文件大小和格式后重试" },
  { keywords: ["network", "timeout", "failed to fetch", "连接", "超时"], message: "网络连接异常，请稍后重试" },
];

const GENERIC_MESSAGES = new Set([
  "参数有误",
  "参数错误",
  "请求参数有误",
  "parameter validation failed",
  "validation failed",
  "invalid request",
  "bad request",
  "request body format is invalid",
  "network error",
  "request failed",
]);

function normalizeRawMessage(input) {
  if (!input) return "";
  if (typeof input === "string") return input.trim();
  if (typeof input.message === "string") return input.message.trim();
  return String(input).trim();
}

function isGenericMessage(message) {
  return GENERIC_MESSAGES.has(message.toLowerCase()) || /^request failed with status code \d+$/i.test(message);
}

function isTechnicalMessage(message) {
  return /exception|stack trace|sql|jdbc|mybatis|dataaccess|dataintegrity|constraint|syntax|axios|^\d+$|^[A-Z_]{3,}$/i.test(message);
}

function getSqlField(message) {
  const match = /column ['"`](.+?)['"`]/i.exec(message) || /for column ['"`](.+?)['"`]/i.exec(message);
  if (!match) return "";
  const raw = match[1];
  return FIELD_ALIAS[raw] || FIELD_ALIAS[raw.toLowerCase()] || raw;
}

function standardizeSqlError(message) {
  if (/out of range value for column/i.test(message)) {
    const field = getSqlField(message);
    return field ? `${field}超出允许范围，请调整后重试` : "数值超出允许范围，请调整后重试";
  }
  if (/data truncation|data too long/i.test(message)) {
    const field = getSqlField(message);
    return field ? `${field}内容过长，请缩短后重试` : "提交内容过长，请缩短后重试";
  }
  if (/duplicate entry/i.test(message)) {
    return "该信息已存在，请更换后重试";
  }
  if (/cannot be null/i.test(message) || (/column ['"`]/i.test(message) && /null/i.test(message))) {
    const field = getSqlField(message);
    return field ? `请填写${field}` : "请补全必填信息";
  }
  if (/foreign key constraint/i.test(message)) {
    return "关联数据不存在或状态已变化，请刷新后重试";
  }
  if (/sql|database|jdbc|mybatis|dataaccess|dataintegrity/i.test(message)) {
    return "数据保存失败，请检查填写内容后重试";
  }
  return "";
}

function matchKeyword(message, code) {
  const lower = message.toLowerCase();
  for (const rule of KEYWORD_RULES) {
    if (rule.code && Number(rule.code) !== Number(code)) continue;
    if (rule.keywords.some((keyword) => lower.includes(String(keyword).toLowerCase()))) {
      return rule.message;
    }
  }
  return "";
}

export function standardizeError(message, code) {
  const rawMessage = normalizeRawMessage(message);
  const normalizedCode = Number(code);
  const fallback = CODE_FALLBACK[normalizedCode] || "操作失败，请稍后重试";

  if (!rawMessage) return fallback;

  const sqlMessage = standardizeSqlError(rawMessage);
  if (sqlMessage) return sqlMessage;

  const keywordMessage = matchKeyword(rawMessage, normalizedCode);
  if (keywordMessage && (isGenericMessage(rawMessage) || isTechnicalMessage(rawMessage))) {
    return keywordMessage;
  }

  if (isGenericMessage(rawMessage) || isTechnicalMessage(rawMessage)) {
    return fallback;
  }

  return rawMessage;
}

export function getErrorMessage(error, fallback = "操作失败，请稍后重试") {
  const data = error?.response?.data;
  const code = data?.code ?? error?.response?.status ?? error?.code;
  const message = error?.userMessage || data?.message || error?.message || fallback;
  return standardizeError(message, code);
}
