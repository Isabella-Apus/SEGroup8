package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.SearchKeywordStat;
import com.segroup8.platform.entity.UserSearchHistory;
import com.segroup8.platform.mapper.SearchKeywordStatMapper;
import com.segroup8.platform.mapper.UserSearchHistoryMapper;
import com.segroup8.platform.service.SearchBehaviorService;
import com.segroup8.platform.vo.SearchHotKeywordVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchBehaviorServiceImpl implements SearchBehaviorService {

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
        recordHotKeyword(normalized);
        Long userId = UserContext.getUserId();
        if (userId != null) {
            recordUserHistory(userId, normalized);
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
        userSearchHistoryMapper.deleteBatchIds(removeIds);
    }

    private void recordHotKeyword(String keyword) {
        LocalDate today = LocalDate.now();
        SearchKeywordStat stat = searchKeywordStatMapper.selectOne(new LambdaQueryWrapper<SearchKeywordStat>()
                .eq(SearchKeywordStat::getKeyword, keyword)
                .eq(SearchKeywordStat::getStatDate, today)
                .last("limit 1"));
        if (stat == null) {
            stat = new SearchKeywordStat();
            stat.setKeyword(keyword);
            stat.setStatDate(today);
            stat.setSearchCount(1);
            searchKeywordStatMapper.insert(stat);
            return;
        }
        stat.setSearchCount(stat.getSearchCount() == null ? 1 : stat.getSearchCount() + 1);
        searchKeywordStatMapper.updateById(stat);
    }

    private String normalize(String keyword) {
        String value = keyword.trim();
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
}
