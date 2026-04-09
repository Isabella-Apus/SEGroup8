package com.segroup8.platform.vo;

import java.util.ArrayList;
import java.util.List;

public class AdminBatchCloseResultVO {

    private List<Long> successIds = new ArrayList<>();
    private List<AdminBatchCloseFailItemVO> failedItems = new ArrayList<>();

    public List<Long> getSuccessIds() {
        return successIds;
    }

    public void setSuccessIds(List<Long> successIds) {
        this.successIds = successIds;
    }

    public List<AdminBatchCloseFailItemVO> getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(List<AdminBatchCloseFailItemVO> failedItems) {
        this.failedItems = failedItems;
    }
}
