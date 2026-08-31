# HPA 实验

负载接口使用 `GET /api/product/search?keyword=java`。预热 2 分钟后逐级施压 5 分钟，采集请求数、吞吐、平均/P95、错误率、Pod 数、CPU 和内存。确认 metrics-server 正常，HPA 初始 2、副本上限 8、CPU 目标 65%。停止负载后观察 300 秒稳定窗口和回缩。

推荐命令：`kubectl get hpa,pod -w -n segroup8` 配合 k6/hey；原始负载输出和 `kubectl top pod` 保存到 `evidence/performance/`。不得编造未运行的数值。
