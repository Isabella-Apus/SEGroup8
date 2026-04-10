package com.segroup8.platform.service.logistics;

import java.util.List;

public interface LogisticsEngine {

    String resolveRegion(String province);

    List<String> generatePathNodes(String originProvince, String destProvince);
}
