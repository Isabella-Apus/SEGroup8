package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.SearchKeywordStat;
import com.segroup8.platform.entity.UserSearchHistory;
import com.segroup8.platform.mapper.SearchKeywordStatMapper;
import com.segroup8.platform.mapper.UserSearchHistoryMapper;
import com.segroup8.platform.service.SearchBehaviorService;
import com.segroup8.platform.vo.SearchHotKeywordVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchBehaviorServiceImpl implements SearchBehaviorService {

    private static final Logger log = LoggerFactory.getLogger(SearchBehaviorServiceImpl.class);
    private static final int MAX_HISTORY = 5;
    private static final int HOT_KEYWORD_LIMIT = 8;

    private final UserSearchHistoryMapper userSearchHistoryMapper;
    private final SearchKeywordStatMapper searchKeywordStatMapper;

    public SearchBehaviorServiceImpl(UserSearchHistoryMapper userSearchHistoryMapper,
            SearchKeywordStatMapper searchKeywordStatMapper) {
        this.userSearchHistoryMapper = userSearchHistoryMapper;
        this.searchKeywordStatMapper = searchKeywordStatMapper;
    }

    @Override
    public void recordKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String normalized = normalize(keyword);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        try {
            recordHotKeyword(normalized);
            Long userId = UserContext.getUserId();
            if (userId != null) {
                recordUserHistory(userId, normalized);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to record search keyword: {}", normalized, ex);
        }
    }

    @Override
    public List<String> getMyHistory() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return List.of();
        }
        List<UserSearchHistory> rows = userSearchHistoryMapper.selectList(new LambdaQueryWrapper<UserSearchHistory>()
                .eq(UserSearchHistory::getUserId, userId)
                .orderByDesc(UserSearchHistory::getSearchTime)
                .last("limit " + MAX_HISTORY));
        return rows.stream().map(UserSearchHistory::getKeyword).toList();
    }

    @Override
    public List<SearchHotKeywordVO> getHotKeywords() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        List<Map<String, Object>> rows = searchKeywordStatMapper.selectMaps(new QueryWrapper<SearchKeywordStat>()
                .select("keyword", "SUM(search_count) AS total_count")
                .ge("stat_date", startDate)
                .groupBy("keyword")
                .orderByDesc("total_count")
                .last("limit " + HOT_KEYWORD_LIMIT));

        List<SearchHotKeywordVO> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            SearchHotKeywordVO vo = new SearchHotKeywordVO();
            vo.setRank(i + 1);
            vo.setKeyword(String.valueOf(row.get("keyword")));
            Object total = row.get("total_count");
            int score = total == null ? 0 : Integer.parseInt(String.valueOf(total));
            vo.setScore(score);
            result.add(vo);
        }
        return result;
    }

    private void recordUserHistory(Long userId, String keyword) {
        userSearchHistoryMapper.delete(new LambdaQueryWrapper<UserSearchHistory>()
                .eq(UserSearchHistory::getUserId, userId)
                .eq(UserSearchHistory::getKeyword, keyword));

        UserSearchHistory history = new UserSearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setSearchTime(LocalDateTime.now());
        userSearchHistoryMapper.insert(history);

        List<UserSearchHistory> all = userSearchHistoryMapper.selectList(new LambdaQueryWrapper<UserSearchHistory>()
                .eq(UserSearchHistory::getUserId, userId)
                .orderByDesc(UserSearchHistory::getSearchTime));
        if (all.size() <= MAX_HISTORY) {
            return;
        }
        List<Long> removeIds = all.subList(MAX_HISTORY, all.size()).stream()
                .map(UserSearchHistory::getId)
                .toList();
        userSearchHistoryMapper.delete(new LambdaQueryWrapper<UserSearchHistory>()
                .in(UserSearchHistory::getId, removeIds));
    }

    private void recordHotKeyword(String keyword) {
        LocalDate today = LocalDate.now();
        int updated = incrementHotKeyword(keyword, today);
        if (updated > 0) {
            return;
        }

        SearchKeywordStat stat = new SearchKeywordStat();
        stat.setKeyword(keyword);
        stat.setStatDate(today);
        stat.setSearchCount(1);
        try {
            searchKeywordStatMapper.insert(stat);
        } catch (DataIntegrityViolationException ex) {
            incrementHotKeyword(keyword, today);
        }
    }

    private int incrementHotKeyword(String keyword, LocalDate statDate) {
        return searchKeywordStatMapper.update(null, new LambdaUpdateWrapper<SearchKeywordStat>()
                .eq(SearchKeywordStat::getKeyword, keyword)
                .eq(SearchKeywordStat::getStatDate, statDate)
                .setSql("search_count = IFNULL(search_count, 0) + 1"));
    }

    private String normalize(String keyword) {
        String value = keyword.trim();
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
}
