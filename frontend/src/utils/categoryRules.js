export const ALL_CATEGORY = "全部分类";

export const productCategories = [
  ALL_CATEGORY,
  "电子数码",
  "服装鞋包",
  "学习办公",
  "生活百货",
  "运动户外",
];

export const secondhandCategories = [
  ALL_CATEGORY,
  "数码闲置",
  "服饰鞋包",
  "教材书籍",
  "宿舍生活",
  "运动器材",
];

export const productQuickLinks = productCategories
  .filter((category) => category !== ALL_CATEGORY)
  .map((category) => ({ label: category, category }));

export const secondhandQuickLinks = secondhandCategories
  .filter((category) => category !== ALL_CATEGORY)
  .map((category) => ({ label: category, category }));

const productRules = {
  电子数码: ["电子数码", "键盘", "鼠标", "显示器", "平板", "扩展坞", "路由器", "耳机", "数码", "keyboard", "mouse", "monitor", "tablet", "headphone"],
  服装鞋包: ["服装鞋包", "衬衫", "托特包", "帆布鞋", "外套", "棒球帽", "针织", "鞋", "包", "服", "shirt", "bag", "shoes", "cap", "jacket"],
  学习办公: ["学习办公", "错题", "文件架", "台灯", "笔袋", "资料", "课程", "椅", "办公", "学习", "desk", "lamp", "office", "chair", "study"],
  生活百货: ["生活百货", "保温杯", "收纳", "香薰", "风扇", "夜灯", "洗衣", "百货", "生活"],
  运动户外: ["运动户外", "瑜伽", "水壶", "毛巾", "羽毛球", "健身", "运动", "户外"],
};

const secondhandRules = {
  数码闲置: ["数码闲置", "显示器", "键盘", "耳机", "平板", "相机", "充电宝", "数码", "monitor", "keyboard", "headphone", "camera", "tablet"],
  服饰鞋包: ["服饰鞋包", "双肩包", "卫衣", "运动鞋", "斜挎包", "围巾", "牛仔", "鞋", "包", "服", "bag", "shoes", "hoodie", "scarf", "jacket"],
  教材书籍: ["教材书籍", "教材", "考研", "词汇", "笔记", "参考书", "小说", "书", "book", "textbook", "notes", "novel"],
  宿舍生活: ["宿舍生活", "书桌", "台灯", "收纳箱", "小桌板", "置物架", "电饭煲", "宿舍", "desk", "lamp", "storage", "dorm"],
  运动器材: ["运动器材", "山地车", "滑板", "哑铃", "羽毛球", "篮球", "露营", "运动", "bicycle", "bike", "skateboard", "basketball", "camping"],
};

export function matchProductCategory(item, category) {
  return matchByRules(item, category, productRules);
}

export function matchSecondhandCategory(item, category) {
  return matchByRules(item, category, secondhandRules);
}

function matchByRules(item, category, rules) {
  if (!category || category === ALL_CATEGORY) {
    return true;
  }
  const direct = item.categoryName || item.category;
  if (direct === category) {
    return true;
  }
  const keywords = rules[category] || [];
  if (!keywords.length) {
    return false;
  }
  const haystack = [
    item.name,
    item.description,
    item.categoryName,
    item.category,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
  return keywords.some((keyword) => haystack.includes(keyword.toLowerCase()));
}
