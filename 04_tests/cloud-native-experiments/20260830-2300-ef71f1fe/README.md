# 20260830-2300-ef71f1fe 原始证据索引

本目录保存云原生与性能实验的仓库内证据。完整结论、边界和复现命令见
`03_devops/cloud-native-experiments/README.md`。

- `environment/`：环境、构件 SHA、部署资源、部署失败与成功日志。
- `performance/`：正式稳定态 3 接口 × 2 架构 × 3 轮。
- `performance-exploratory-overload-20/`：并发 20 探索性过载，不用于正式中位数。
- `hpa/`：并发 10 的正式 HPA 调优轮次。
- `hpa-overload-60/`：并发 60 的三轮过载边界。
- `dependency-fault/`：订单依赖停止、受控响应、健康检查和恢复失败证据。

状态摘要：HPA 扩缩容通过；课程依赖故障最低要求通过；自动补偿恢复未通过；
性能对比完成。任何结论都应以各目录的 `summary.json` 和原始文件为准。
