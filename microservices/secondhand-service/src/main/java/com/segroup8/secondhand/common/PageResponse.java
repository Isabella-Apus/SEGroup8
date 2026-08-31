package com.segroup8.secondhand.common;

import java.util.List;

public record PageResponse<T>(long total, long pageNum, long pageSize, List<T> records) {
}
