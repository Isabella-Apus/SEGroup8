/**
 * 搜索模块：统一导出模糊搜索能力（基于 Fuse.js）。
 * 推荐页面优先使用 searchService 暴露的通用接口。
 */
export { fuzzySearchItems, fuzzySearchBrowseHistory } from "./fuseSearch";
export { searchList, searchBrowseHistory } from "./searchService";
export { SEARCH_SCENARIOS_DOC } from "./scenarios";
