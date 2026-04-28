package com.segroup8.platform.service;

import com.segroup8.platform.vo.SearchHotKeywordVO;

import java.util.List;

public interface SearchBehaviorService {

    void recordKeyword(String keyword);

    List<String> getMyHistory();

    List<SearchHotKeywordVO> getHotKeywords();
}
