import Fuse from "fuse.js";

/**
 * Fuse.js（Bitap）模糊匹配默认配置。
 */
const DEFAULT_FUSE_OPTIONS = {
  includeScore: true,
  shouldSort: true,
  threshold: 0.35,
  ignoreLocation: true,
  minMatchCharLength: 1,
  isCaseSensitive: false,
  ignoreDiacritics: true,
};

/**
 * 轻量拼音映射（字符级）。
 */
const HANZI_TO_PINYIN = {
  键: "jian",
  盘: "pan",
  鼠: "shu",
  标: "biao",
  显: "xian",
  示: "shi",
  器: "qi",
  耳: "er",
  机: "ji",
  手: "shou",
  电: "dian",
  脑: "nao",
  台: "tai",
  式: "shi",
  笔: "bi",
  记: "ji",
  本: "ben",
  充: "chong",
  线: "xian",
  摄: "she",
  像: "xiang",
  头: "tou",
  音: "yin",
  箱: "xiang",
  自: "zi",
  行: "xing",
  车: "che",
  山: "shan",
  地: "di",
  路: "lu",
  由: "you",
  平: "ping",
  板: "ban",
  桌: "zhuo",
  灯: "deng",
  二: "er",
  新: "xin",
  品: "pin",
};

/**
 * 通用品类别名（非具体商品词），新增商品通常不需要维护。
 */
const EN_TO_ZH_ALIAS = {
  keyboard: ["键盘"],
  mouse: ["鼠标"],
  monitor: ["显示器"],
  laptop: ["笔记本", "电脑"],
  notebook: ["笔记本"],
  phone: ["手机"],
  mobile: ["手机"],
  headset: ["耳机"],
  earphone: ["耳机"],
  headphones: ["耳机"],
  speaker: ["音箱"],
  camera: ["摄像头"],
  charger: ["充电器", "充电线"],
  bike: ["自行车"],
  bicycle: ["自行车"],
  tablet: ["平板"],
  desk: ["书桌"],
  lamp: ["台灯"],
};

const ZH_TO_EN_ALIAS = {
  键盘: ["keyboard"],
  鼠标: ["mouse"],
  显示器: ["monitor"],
  笔记本: ["laptop", "notebook"],
  电脑: ["computer", "pc"],
  手机: ["phone", "mobile"],
  耳机: ["headset", "earphone", "headphones"],
  音箱: ["speaker"],
  摄像头: ["camera"],
  充电器: ["charger"],
  充电线: ["charger"],
  自行车: ["bike", "bicycle"],
  山地车: ["bike", "bicycle"],
  平板: ["tablet"],
  书桌: ["desk"],
  台灯: ["lamp"],
};

const ZH_TO_ZH_ALIAS = {
  机械键盘: ["键盘", "电子数码", "数码闲置"],
  平板: ["学习平板", "电子数码", "数码闲置"],
  教材: ["教材书籍", "教材", "课程资料", "学习办公"],
  宿舍收纳: ["收纳", "收纳盒", "宿舍生活", "生活百货"],
  运动耳机: ["耳机", "运动户外", "运动器材"],
  数码: ["电子数码", "数码闲置", "键盘", "耳机", "平板"],
  办公: ["学习办公", "文件架", "台灯", "办公椅"],
  学习: ["学习办公", "教材书籍", "课程资料", "笔记"],
  生活: ["生活百货", "宿舍生活", "收纳", "台灯"],
  宿舍: ["宿舍生活", "生活百货", "收纳", "小桌板"],
  运动: ["运动户外", "运动器材", "羽毛球", "健身", "耳机"],
  户外: ["运动户外", "运动器材", "露营"],
  耳机: ["耳机", "运动耳机", "电子数码", "数码闲置"],
};

// 常用拼音词 -> 中文词（用于 jianpan / erji / zixingche 等输入）
const PINYIN_TO_ZH_ALIAS = {
  jianpan: ["键盘"],
  shubiao: ["鼠标"],
  xianshiqi: ["显示器"],
  bijiben: ["笔记本"],
  diannao: ["电脑"],
  shouji: ["手机"],
  erji: ["耳机"],
  yinxiang: ["音箱"],
  shexiangtou: ["摄像头"],
  chongdianqi: ["充电器", "充电线"],
  zixingche: ["自行车", "山地车"],
  shandiche: ["山地车", "自行车"],
  pingban: ["平板"],
  shuzhuo: ["书桌"],
  taideng: ["台灯"],
};

const CHINESE_STOP_CHARS = new Set(["二", "手", "新", "品", "的", "了", "和", "与", "及"]);

// 基于 items 引用的轻量缓存：避免同一页反复输入/回删时重复构建 Fuse 查询。
const SEARCH_CACHE = new WeakMap();
const CACHE_LIMIT_PER_ITEMS = 40;

function normalizeText(text) {
  return String(text || "")
    .toLowerCase()
    .replace(/[\u3000\s]+/g, " ")
    .trim();
}

function containsChinese(text) {
  return /[\u3400-\u9fff]/.test(text);
}

function toPinyinLoose(text) {
  const raw = String(text || "");
  const fullParts = [];
  const spacedParts = [];

  for (const ch of raw) {
    if (HANZI_TO_PINYIN[ch]) {
      const py = HANZI_TO_PINYIN[ch];
      fullParts.push(py);
      spacedParts.push(py);
      continue;
    }

    if (/[a-z0-9]/i.test(ch)) {
      const c = ch.toLowerCase();
      fullParts.push(c);
      spacedParts.push(c);
    }
  }

  return {
    full: fullParts.join(""),
    spaced: spacedParts.join(" "),
  };
}

function singularizeEn(word) {
  const w = String(word || "").toLowerCase();
  if (w.endsWith("ies") && w.length > 3) return `${w.slice(0, -3)}y`;
  if (w.endsWith("es") && w.length > 2) return w.slice(0, -2);
  if (w.endsWith("s") && w.length > 1) return w.slice(0, -1);
  return w;
}

function readByPath(obj, path) {
  return String(path)
    .split(".")
    .reduce((acc, key) => (acc == null ? undefined : acc[key]), obj);
}

function buildSearchDocs(items, keys) {
  return (items || []).map((item, idx) => {
    const sourceText = [];

    (keys || []).forEach((key) => {
      if (typeof key === "string") {
        const value = readByPath(item, key);
        if (value != null) sourceText.push(String(value));
        return;
      }

      if (key && typeof key.getFn === "function") {
        const value = key.getFn(item);
        if (value != null) sourceText.push(String(value));
      }
    });

    const raw = normalizeText(sourceText.join(" "));
    const compact = raw.replace(/\s+/g, "");
    const py = toPinyinLoose(raw);

    return {
      ...(item || {}),
      __search_index: idx,
      __search_raw: raw,
      __search_compact: compact,
      __search_pinyin: py.full,
      __search_pinyin_spaced: py.spaced,
    };
  });
}

function buildQueries(keyword) {
  const normalized = normalizeText(keyword);
  if (!normalized) return [];

  const queries = new Set([normalized, normalized.replace(/\s+/g, "")]);

  if (/^[a-z\s]+$/i.test(normalized)) {
    const tokens = normalized.split(/\s+/).filter(Boolean);
    tokens.forEach((t) => {
      const base = singularizeEn(t);
      queries.add(base);

      // 英文词 -> 中文别名
      (EN_TO_ZH_ALIAS[t] || []).forEach((alias) => queries.add(alias));
      (EN_TO_ZH_ALIAS[base] || []).forEach((alias) => queries.add(alias));

      // 拼音词 -> 中文别名（支持 jianpan / erji 等）
      (PINYIN_TO_ZH_ALIAS[t] || []).forEach((alias) => {
        queries.add(alias);
        (ZH_TO_EN_ALIAS[alias] || []).forEach((enAlias) => {
          queries.add(enAlias);
          queries.add(`${enAlias}s`);
        });
      });
      (PINYIN_TO_ZH_ALIAS[base] || []).forEach((alias) => {
        queries.add(alias);
        (ZH_TO_EN_ALIAS[alias] || []).forEach((enAlias) => {
          queries.add(enAlias);
          queries.add(`${enAlias}s`);
        });
      });
    });
  }

  if (containsChinese(normalized)) {
    Object.keys(ZH_TO_ZH_ALIAS).forEach((zh) => {
      if (normalized.includes(zh) || zh.includes(normalized)) {
        ZH_TO_ZH_ALIAS[zh].forEach((alias) => queries.add(alias));
      }
    });

    Object.keys(ZH_TO_EN_ALIAS).forEach((zh) => {
      // 支持中文“前缀/子串”触发中英扩展：如“键”也能触发“键盘 -> keyboard”
      if (normalized.includes(zh) || zh.includes(normalized)) {
        ZH_TO_EN_ALIAS[zh].forEach((alias) => {
          queries.add(alias);
          queries.add(`${alias}s`);
        });
      }
    });

    const py = toPinyinLoose(normalized);
    if (py.full) queries.add(py.full);
    if (py.spaced) queries.add(py.spaced);
  }

  return Array.from(queries).filter(Boolean);
}

function passChineseGuard(query, doc) {
  if (!containsChinese(query)) return true;

  const haystack = String(doc.__search_raw || "");
  const hasChineseInDoc = containsChinese(haystack);

  // 文档本身是英文时，不做中文字符交集限制（靠中英别名兜底）
  if (!hasChineseInDoc) return true;

  const chars = Array.from(query).filter((ch) => /[\u3400-\u9fff]/.test(ch) && !CHINESE_STOP_CHARS.has(ch));
  if (!chars.length) return true;

  return chars.some((ch) => haystack.includes(ch));
}

function getPrefixRank(query, doc) {
  const q = normalizeText(query);
  if (!q) return 2;

  const qCompact = q.replace(/\s+/g, "");
  const raw = normalizeText(doc?.__search_raw || "");
  const compact = normalizeText(doc?.__search_compact || "");

  if (raw.startsWith(q) || compact.startsWith(qCompact)) return 0;
  if (raw.includes(q) || compact.includes(qCompact)) return 1;
  return 2;
}

function getCacheBucket(items) {
  if (!Array.isArray(items)) return null;
  let bucket = SEARCH_CACHE.get(items);
  if (!bucket) {
    bucket = new Map();
    SEARCH_CACHE.set(items, bucket);
  }
  return bucket;
}

function buildCacheKey(normalized, options) {
  const keys = (options?.keys || ["name", "description"]).map((k) => {
    if (typeof k === "string") return `s:${k}`;
    return `o:${k?.name || "fn"}`;
  });
  const threshold = options?.threshold ?? DEFAULT_FUSE_OPTIONS.threshold;
  return `${normalized}|t:${threshold}|k:${keys.join(",")}`;
}

function saveCache(bucket, key, value) {
  if (!bucket) return;
  if (bucket.size >= CACHE_LIMIT_PER_ITEMS) {
    const firstKey = bucket.keys().next().value;
    if (firstKey !== undefined) bucket.delete(firstKey);
  }
  bucket.set(key, value);
}

/**
 * 通用列表模糊搜索（商品名、描述等扁平字段）。
 */
export function fuzzySearchItems(items, keyword, options = {}) {
  const normalized = normalizeText(keyword);
  if (!normalized) {
    return items;
  }

  if (!Array.isArray(items) || !items.length) {
    return [];
  }

  const bucket = getCacheBucket(items);
  const cacheKey = buildCacheKey(normalized, options);
  if (bucket?.has(cacheKey)) {
    return bucket.get(cacheKey);
  }

  const { keys, threshold, ...rest } = options;
  const activeKeys = keys || ["name", "description"];
  const docs = buildSearchDocs(items, activeKeys);
  const queries = buildQueries(normalized);

  const fuse = new Fuse(docs, {
    ...DEFAULT_FUSE_OPTIONS,
    ...(threshold != null ? { threshold } : {}),
    keys: [
      ...activeKeys.filter((k) => typeof k === "string"),
      "__search_raw",
      "__search_compact",
      "__search_pinyin",
      "__search_pinyin_spaced",
    ],
    ...rest,
  });

  const best = new Map();

  queries.forEach((q) => {
    fuse.search(q).forEach((row) => {
      const idx = row.item.__search_index;
      if (!passChineseGuard(normalized, row.item)) return;

      const current = best.get(idx);
      const nextScore = Number(row.score ?? 1);
      const nextPrefixRank = getPrefixRank(normalized, row.item);
      if (
        !current
        || nextPrefixRank < current.prefixRank
        || (nextPrefixRank === current.prefixRank && nextScore < current.score)
      ) {
        best.set(idx, {
          score: nextScore,
          prefixRank: nextPrefixRank,
          value: (items || [])[idx],
        });
      }
    });
  });

  const result = Array.from(best.values())
    .sort((a, b) => (a.prefixRank - b.prefixRank) || (a.score - b.score))
    .map((x) => x.value)
    .filter(Boolean);

  saveCache(bucket, cacheKey, result);
  return result;
}

/**
 * 浏览记录：按商品名称、商品 ID、类型匹配。
 */
export function fuzzySearchBrowseHistory(records, keyword) {
  return fuzzySearchItems(records, keyword, {
    keys: [
      "product.name",
      "product.description",
      {
        name: "productId",
        getFn: (doc) => String(doc.product?.id ?? ""),
      },
      {
        name: "productType",
        getFn: (doc) => String(doc.product?.type || doc.productType || ""),
      },
    ],
    threshold: 0.35,
  });
}
