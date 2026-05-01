package com.segroup8.platform.service.logistics;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultLogisticsEngine extends AbstractLogisticsEngine {

    private static final String HUABEI = "华北";
    private static final String DONGBEI = "东北";
    private static final String HUADONG = "华东";
    private static final String HUANAN = "华南";
    private static final String HUAZHONG = "华中";
    private static final String XINAN = "西南";
    private static final String XIBEI = "西北";

    private static final Map<String, String> PROVINCE_REGION = new HashMap<>();
    private static final Map<String, List<String>> REGION_GRAPH = new HashMap<>();
    private static final Map<String, String> REGION_HUB = new HashMap<>();

    static {
        // 华北
        bind(HUABEI, "北京", "天津", "河北", "山西", "内蒙古", "beijing", "tianjin", "hebei", "shanxi", "neimenggu");
        // 东北
        bind(DONGBEI, "辽宁", "吉林", "黑龙江", "沈阳", "大连", "liaoning", "jilin", "heilongjiang", "shenyang");
        // 华东
        bind(HUADONG, "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东", "shanghai", "jiangsu", "zhejiang", "anhui", "fujian",
                "jiangxi", "shandong");
        // 华南
        bind(HUANAN, "广东", "广西", "海南", "guangdong", "guangxi", "hainan");
        // 华中
        bind(HUAZHONG, "河南", "湖北", "湖南", "henan", "hubei", "hunan", "武汉", "wuhan");
        // 西南
        bind(XINAN, "重庆", "四川", "贵州", "云南", "西藏", "chongqing", "sichuan", "guizhou", "yunnan", "xizang", "成都",
                "chengdu");
        // 西北
        bind(XIBEI, "陕西", "甘肃", "青海", "宁夏", "新疆", "shanxi2", "shaanxi", "gansu", "qinghai", "ningxia", "xinjiang", "西安",
                "xian");

        REGION_GRAPH.put(HUABEI, List.of(DONGBEI, HUADONG, HUAZHONG, XIBEI));
        REGION_GRAPH.put(DONGBEI, List.of(HUABEI));
        REGION_GRAPH.put(HUADONG, List.of(HUABEI, HUAZHONG, HUANAN));
        REGION_GRAPH.put(HUANAN, List.of(HUADONG, HUAZHONG, XINAN));
        REGION_GRAPH.put(HUAZHONG, List.of(HUABEI, HUADONG, HUANAN, XINAN, XIBEI));
        REGION_GRAPH.put(XINAN, List.of(HUAZHONG, HUANAN, XIBEI));
        REGION_GRAPH.put(XIBEI, List.of(HUABEI, HUAZHONG, XINAN));

        REGION_HUB.put(HUABEI, "北京");
        REGION_HUB.put(DONGBEI, "北京");
        REGION_HUB.put(HUADONG, "上海");
        REGION_HUB.put(HUANAN, "广州");
        REGION_HUB.put(HUAZHONG, "武汉");
        REGION_HUB.put(XINAN, "成都");
        REGION_HUB.put(XIBEI, "西安");
    }

    @Override
    public String resolveRegion(String province) {
        if (province == null || province.isBlank()) {
            return null;
        }
        String key = province.trim().toLowerCase();
        String region = PROVINCE_REGION.get(key);
        if (region != null) {
            return region;
        }
        // 简单兼容“xx省/xx市/自治区”后缀
        key = key.replace("省", "").replace("市", "").replace("壮族自治区", "")
                .replace("回族自治区", "").replace("维吾尔自治区", "").replace("自治区", "");
        return PROVINCE_REGION.get(key);
    }

    @Override
    protected List<String> adjacentRegions(String region) {
        return REGION_GRAPH.getOrDefault(region, List.of());
    }

    @Override
    protected String transitHubByRegion(String region) {
        return REGION_HUB.get(region);
    }

    private static void bind(String region, String... provinceNames) {
        for (String provinceName : provinceNames) {
            if (provinceName == null) {
                continue;
            }
            PROVINCE_REGION.put(provinceName.trim().toLowerCase(), region);
        }
    }
}
