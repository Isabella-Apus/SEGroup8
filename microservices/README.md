# UC06-UC10 微服务

## 服务与端口

| 服务 | 端口 | Schema | 用例 |
|---|---:|---|---|
| catalog-service | 8081 | catalog_schema | UC06、UC07 |
| shop-service | 8082 | shop_schema | UC08 |
| risk-service | 8083 | risk_schema | UC09 |
| behavior-service | 8084 | behavior_schema | UC10 |

## 本机验证

需要 JDK 17 和 Maven 3.9：

```powershell
cd microservices
mvn clean test
```

每个服务也可以独立构建：

```powershell
mvn -pl catalog-service -am package
mvn -pl shop-service -am package
mvn -pl risk-service -am package
mvn -pl behavior-service -am package
```

测试使用 H2 的 MySQL 兼容模式，不依赖外部数据库。生产运行使用 MySQL 8，并通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 注入连接信息。接口文档位于各服务的 `/v3/api-docs` 和 `/swagger-ui.html`。

## Kubernetes

```powershell
kubectl apply -f k8s/namespace.yaml
kubectl -n se-group8 create secret generic commerce-db-secret `
  --from-literal=root-password='<安全口令>' `
  --from-literal=catalog-password='<安全口令>' `
  --from-literal=shop-password='<安全口令>' `
  --from-literal=risk-password='<安全口令>' `
  --from-literal=behavior-password='<安全口令>'
kubectl apply -k k8s
kubectl -n se-group8 get pods,svc
kubectl -n se-group8 port-forward svc/catalog-service 8081:8081
```

仓库不保存数据库口令。部署前必须在目标命名空间创建 `commerce-db-secret`，正式环境建议由平台 Secret 或外部密钥管理服务提供。

Docker 构建上下文必须是 `microservices` 目录：

```powershell
docker build -f catalog-service/Dockerfile -t se-group8/catalog-service:1.0.0 .
docker build -f shop-service/Dockerfile -t se-group8/shop-service:1.0.0 .
docker build -f risk-service/Dockerfile -t se-group8/risk-service:1.0.0 .
docker build -f behavior-service/Dockerfile -t se-group8/behavior-service:1.0.0 .
```
