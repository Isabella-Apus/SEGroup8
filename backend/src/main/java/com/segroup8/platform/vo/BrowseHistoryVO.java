package com.segroup8.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrowseHistoryVO {
    private Long id;

    private ProductVO product;

    private String productType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime browseTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVO {
        private Long id;
        private String name;
        private java.math.BigDecimal price;
        private String cover;
    }
}
