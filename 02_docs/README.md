# 02_docs 最终文档索引

本目录仅保留课程最终验收需要的需求、设计、模型、测试说明和微服务改造材料。Issue/PR 描述、阶段审计、迁移过程稿、中期稿和重复证据已移到仓库外 `selfwork/SEGroup8-final-prune-20260902/`。

## 系统级文档

- [软件需求说明书](specifications/software-requirements.md)
- [软件概要设计说明书](specifications/software-architecture-design.md)
- [软件详细设计说明书](specifications/software-detailed-design.md)
- [需求追溯矩阵](specifications/requirements-traceability-matrix.md)
- [UC01-UC25 总清单](use-case-catalog.md)
- [最终测试汇总](test-summary.md)

## 微服务改造必交材料

- [服务划分与调用关系图](architecture/microservice-boundaries.md)
- [服务接口清单](architecture/service-api-catalog.md)
- [数据表归属表](architecture/database-ownership.md)
- [跨服务调用规则](architecture/README.md)
- `microservices/<service>/`：六个服务各自的边界、图、OpenAPI、表归属、跨服务调用、改造前后差异与追溯。

## 用例文档

`UC01/` 至 `UC25/` 各保留需求、六类模型源文件、测试计划、测试报告和追溯。原始结构化测试结果与关键截图位于 `../04_tests/`；完整 HTML、trace、video 由 Actions artifact 保存。
