# 资金对账手册

## 日终余额与流水

若迁移时导入了期初余额，应存为 `OPENING_BALANCE` 流水。完成迁移后使用：

```sql
SELECT b.user_id,
       b.personal_balance,
       COALESCE(SUM(CASE WHEN t.account_type='PERSONAL' THEN t.amount ELSE 0 END), 0) AS personal_ledger,
       b.business_balance,
       COALESCE(SUM(CASE WHEN t.account_type='BUSINESS' THEN t.amount ELSE 0 END), 0) AS business_ledger
FROM balance b
LEFT JOIN transaction_record t ON t.user_id=b.user_id
GROUP BY b.user_id, b.personal_balance, b.business_balance
HAVING b.personal_balance <> personal_ledger OR b.business_balance <> business_ledger;
```

结果必须为空。若存在差异：暂停相关账户出账、保全日志和数据库快照、按业务请求追踪，不删除或篡改既有流水；经审批后写可追踪的更正流水。

## 请求完整性

```sql
SELECT p.request_id, p.status, p.transaction_id
FROM payment_request p
LEFT JOIN transaction_record t ON t.transaction_id=p.transaction_id
WHERE p.status='COMPLETED' AND t.transaction_id IS NULL;

SELECT business_request_id, COUNT(*)
FROM transaction_record
GROUP BY business_request_id
HAVING COUNT(*) > 1;
```

两条结果均必须为空。Outbox 长时间积压单独告警，不直接修改资金余额。
