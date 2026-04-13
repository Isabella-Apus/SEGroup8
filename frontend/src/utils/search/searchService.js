import { fuzzySearchItems, fuzzySearchBrowseHistory } from "./fuseSearch";

/**
 * 对外统一搜索服务：页面仅调用本文件，不直接依赖 Fuse 细节。
 */
export function searchList({ items = [], keyword = "", keys = ["name", "description"], options = {} } = {}) {
  const q = String(keyword || "").trim();
  if (!q) return items;
  return fuzzySearchItems(items, q, { keys, ...options });
}

/**
 * 浏览记录搜索（支持名称、ID、类型，并复用拼音/中英容错能力）。
 */
export function searchBrowseHistory({ records = [], keyword = "", options = {} } = {}) {
  const q = String(keyword || "").trim();
  if (!q) return records;
  return fuzzySearchBrowseHistory(records, q, options);
}
