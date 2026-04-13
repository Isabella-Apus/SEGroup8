package com.segroup8.platform.search;

import java.util.List;

/**
 * 模糊搜索算法适配接口，便于后续替换不同实现（Fuse/ES/Lucene 等）。
 */
public interface FuzzySearchAdapter {

    List<String> fuzzySearch(List<String> data, String keyword);
}
