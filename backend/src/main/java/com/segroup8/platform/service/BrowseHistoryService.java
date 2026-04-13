package com.segroup8.platform.service;

import com.segroup8.platform.vo.BrowseHistoryVO;

import java.util.List;

public interface BrowseHistoryService {
    /**
     * Save browse history
     */
    void saveBrowseHistory(Long productId, String productType);

    /**
     * Get current user browse history
     */
    List<BrowseHistoryVO> getBrowseHistory();

    /**
     * Delete one browse history record of current user
     */
    void deleteBrowseHistory(Long historyId);

    /**
     * Batch delete browse history records of current user
     */
    void deleteBrowseHistoryBatch(List<Long> historyIds);

    /**
     * Delete all browse history records of current user
     */
    void clearBrowseHistory();
}
