package com.segroup8.secondhand.repository;

import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepository {
    private final NamedParameterJdbcTemplate db;

    public IdempotencyRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public boolean recordOnce(String scope, String key, String reference) {
        try {
            db.update("insert into idempotency_record(scope_name,idempotency_key,response_reference) "
                            + "values(:scope,:key,:reference)",
                    Map.of("scope", scope, "key", key, "reference", reference == null ? "" : reference));
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    public void release(String scope, String key) {
        db.update("delete from idempotency_record where scope_name=:scope and idempotency_key=:key",
                Map.of("scope", scope, "key", key));
    }
}
