export const CATEGORY_TREE = [
  {
    value: 1,
    label: '电子数码',
    icon: 'phone',
    children: [
      { value: 101, label: '手机' },
      { value: 102, label: '电脑/平板' },
      { value: 103, label: '摄影摄像' },
      { value: 104, label: '影音娱乐' },
      { value: 105, label: '智能穿戴' },
    ],
  },
  {
    value: 2,
    label: '服饰鞋包',
    icon: 'bag',
    children: [
      { value: 201, label: '潮流女装' },
      { value: 202, label: '品质男装' },
      { value: 203, label: '运动户外' },
      { value: 204, label: '精选箱包' },
      { value: 205, label: '时尚饰品' },
    ],
  },
  {
    value: 3,
    label: '居家生活',
    icon: 'home',
    children: [
      { value: 301, label: '家具家装' },
      { value: 302, label: '厨房用具' },
      { value: 303, label: '居家日用' },
      { value: 304, label: '家用电器' },
      { value: 305, label: '收纳整理' },
    ],
  },
  {
    value: 4,
    label: '美妆个护',
    icon: 'makeup',
    children: [
      { value: 401, label: '面部护肤' },
      { value: 402, label: '魅力彩妆' },
      { value: 403, label: '个人护理' },
      { value: 404, label: '香水香氛' },
      { value: 405, label: '美容仪器' },
    ],
  },
  {
    value: 5,
    label: '运动户外',
    icon: 'sport',
    children: [
      { value: 501, label: '健身器材' },
      { value: 502, label: '户外装备' },
      { value: 503, label: '体育用品' },
      { value: 504, label: '骑行运动' },
    ],
  },
  {
    value: 6,
    label: '图书音像',
    icon: 'book',
    children: [
      { value: 601, label: '教材教辅' },
      { value: 602, label: '小说文学' },
      { value: 603, label: '艺术收藏' },
      { value: 604, label: '办公用品' },
    ],
  },
  {
    value: 7,
    label: '美食类',
    icon: 'food',
    children: [
      { value: 701, label: '休闲零食' },
      { value: 702, label: '粮油调味' },
      { value: 703, label: '生鲜果蔬' },
      { value: 704, label: '冲调饮品' },
      { value: 705, label: '地方特产' },
    ],
  },
  {
    value: 8,
    label: '其他',
    icon: 'more',
    children: [
      { value: 801, label: '未分类' },
    ],
  },
];

export const MAIN_CATEGORY_OPTIONS = CATEGORY_TREE.map((item) => ({
  label: item.label,
  value: item.value,
}));

export const SECONDHAND_CATEGORY_TREE = CATEGORY_TREE.filter((item) => item.value !== 7);

export function findMainCategory(categoryId) {
  return CATEGORY_TREE.find((item) => item.value === Number(categoryId));
}

export function cascaderForMainCategory(categoryId, { includeFood = true } = {}) {
  const tree = includeFood ? CATEGORY_TREE : SECONDHAND_CATEGORY_TREE;
  const hit = tree.find((item) => item.value === Number(categoryId));
  return hit ? [hit] : [];
}

export function buildCategoryPath(categoryId, subCategoryId) {
  if (!categoryId || !subCategoryId) {
    return [];
  }
  return [Number(categoryId), Number(subCategoryId)];
}
