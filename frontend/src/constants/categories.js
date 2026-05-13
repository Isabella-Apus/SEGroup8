export const CATEGORY_TREE = [
  {
    value: 1,
    label: '电子数码',
    icon: 'phone',
    children: [
      { value: 101, label: '手机' },
      { value: 102, label: '电脑/平板' },
      { value: 103, label: '相机影像' },
      { value: 104, label: '影音设备' },
      { value: 105, label: '数码配件' },
    ],
  },
  {
    value: 2,
    label: '服饰鞋包',
    icon: 'bag',
    children: [
      { value: 201, label: '女装' },
      { value: 202, label: '男装' },
      { value: 203, label: '运动服饰' },
      { value: 204, label: '鞋靴箱包' },
      { value: 205, label: '配饰' },
    ],
  },
  {
    value: 3,
    label: '家居生活',
    icon: 'home',
    children: [
      { value: 301, label: '家具' },
      { value: 302, label: '家用电器' },
      { value: 303, label: '家纺装饰' },
      { value: 304, label: '厨具餐具' },
      { value: 305, label: '收纳日用' },
    ],
  },
  {
    value: 4,
    label: '美妆个护',
    icon: 'makeup',
    children: [
      { value: 401, label: '护肤' },
      { value: 402, label: '彩妆' },
      { value: 403, label: '个人护理' },
      { value: 404, label: '香水香氛' },
      { value: 405, label: '美妆工具' },
    ],
  },
  {
    value: 5,
    label: '运动户外',
    icon: 'sport',
    children: [
      { value: 501, label: '健身训练' },
      { value: 502, label: '户外装备' },
      { value: 503, label: '球类运动' },
      { value: 504, label: '骑行运动' },
    ],
  },
  {
    value: 6,
    label: '图书音像',
    icon: 'book',
    children: [
      { value: 601, label: '文学小说' },
      { value: 602, label: '教育考试' },
      { value: 603, label: '音像制品' },
      { value: 604, label: '文具用品' },
    ],
  },
  {
    value: 7,
    label: '食品生鲜',
    icon: 'food',
    children: [
      { value: 701, label: '休闲零食' },
      { value: 702, label: '饮料冲调' },
      { value: 703, label: '生鲜食品' },
      { value: 704, label: '茶酒咖啡' },
      { value: 705, label: '营养保健' },
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

export function findCategoryLabel(categoryId) {
  const main = findMainCategory(categoryId);
  if (main) {
    return main.label;
  }
  for (const item of CATEGORY_TREE) {
    const child = item.children?.find((sub) => sub.value === Number(categoryId));
    if (child) {
      return child.label;
    }
  }
  return '';
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
