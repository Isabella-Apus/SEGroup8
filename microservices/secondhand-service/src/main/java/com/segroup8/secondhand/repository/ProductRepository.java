package com.segroup8.secondhand.repository;

import com.segroup8.secondhand.domain.SecondhandProduct;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
    private static final RowMapper<SecondhandProduct> PRODUCT_MAPPER = ProductRepository::mapProduct;
    private final NamedParameterJdbcTemplate db;

    public ProductRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public Optional<SecondhandProduct> findById(long id) {
        return db.query("select * from secondhand_product where id=:id and deleted=0",
                Map.of("id", id), PRODUCT_MAPPER).stream().findFirst();
    }

    public Optional<SecondhandProduct> findPublicById(long id) {
        return db.query("select * from secondhand_product where id=:id and deleted=0 "
                        + "and risk_status='APPROVED' and status in (1,4)",
                Map.of("id", id), PRODUCT_MAPPER).stream().findFirst();
    }

    public ProductPage searchPublic(long pageNum, long pageSize, String keyword, Integer categoryId,
            BigDecimal minPrice, BigDecimal maxPrice, String conditionLevel, Integer negotiable,
            Long sellerUserId, String sortBy) {
        StringBuilder where = new StringBuilder(" where deleted=0 and risk_status='APPROVED' and status=1");
        Map<String, Object> params = new HashMap<>();
        appendFilters(where, params, keyword, categoryId, minPrice, maxPrice, conditionLevel, negotiable);
        if (sellerUserId != null) {
            where.append(" and seller_user_id=:sellerUserId");
            params.put("sellerUserId", sellerUserId);
        }
        String order = switch (sortBy == null ? "" : sortBy) {
            case "priceAsc" -> " order by sale_price asc, id desc";
            case "priceDesc" -> " order by sale_price desc, id desc";
            default -> " order by create_time desc, id desc";
        };
        return page(where.toString(), params, order, pageNum, pageSize);
    }

    public ProductPage searchSeller(long sellerUserId, long pageNum, long pageSize, String keyword,
            Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String conditionLevel,
            Integer negotiable, Integer status, String sortBy) {
        StringBuilder where = new StringBuilder(" where deleted=0 and seller_user_id=:sellerUserId");
        Map<String, Object> params = new HashMap<>();
        params.put("sellerUserId", sellerUserId);
        appendFilters(where, params, keyword, categoryId, minPrice, maxPrice, conditionLevel, negotiable);
        if (status != null) {
            where.append(" and status=:status");
            params.put("status", status);
        }
        String order = "priceAsc".equals(sortBy) ? " order by sale_price asc, id desc"
                : "priceDesc".equals(sortBy) ? " order by sale_price desc, id desc"
                : " order by create_time desc, id desc";
        return page(where.toString(), params, order, pageNum, pageSize);
    }

    public long insert(long sellerUserId, String sellerName, String name, String cover, String imagesJson,
            String description, BigDecimal originPrice, BigDecimal salePrice, int categoryId,
            int subCategoryId, String conditionLevel, int negotiable, int requestedStatus) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sellerUserId", sellerUserId).addValue("sellerName", sellerName)
                .addValue("name", name).addValue("cover", cover).addValue("images", imagesJson)
                .addValue("description", description).addValue("originPrice", originPrice)
                .addValue("salePrice", salePrice).addValue("categoryId", categoryId)
                .addValue("subCategoryId", subCategoryId).addValue("conditionLevel", conditionLevel)
                .addValue("negotiable", negotiable).addValue("status", requestedStatus);
        db.update("insert into secondhand_product(seller_user_id,seller_name_snapshot,name,cover,images,description,"
                        + "origin_price,sale_price,category_id,sub_category_id,condition_level,is_negotiable,status,risk_status) "
                        + "values(:sellerUserId,:sellerName,:name,:cover,:images,:description,:originPrice,:salePrice,"
                        + ":categoryId,:subCategoryId,:conditionLevel,:negotiable,:status,'RISK_PENDING')",
                params, keyHolder, new String[] {"id"});
        return keyHolder.getKey().longValue();
    }

    public int updateOwned(SecondhandProduct current, String name, String cover, String imagesJson,
            String description, BigDecimal originPrice, BigDecimal salePrice, int categoryId,
            int subCategoryId, String conditionLevel, int negotiable, int requestedStatus) {
        return db.update("update secondhand_product set name=:name,cover=:cover,images=:images,description=:description,"
                        + "origin_price=:originPrice,sale_price=:salePrice,category_id=:categoryId,sub_category_id=:subCategoryId,"
                        + "condition_level=:conditionLevel,is_negotiable=:negotiable,status=:status,risk_status='RISK_PENDING',"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP where id=:id and seller_user_id=:sellerUserId "
                        + "and version=:version and deleted=0 and status in (1,2)",
                new MapSqlParameterSource().addValue("name", name).addValue("cover", cover)
                        .addValue("images", imagesJson).addValue("description", description)
                        .addValue("originPrice", originPrice).addValue("salePrice", salePrice)
                        .addValue("categoryId", categoryId).addValue("subCategoryId", subCategoryId)
                        .addValue("conditionLevel", conditionLevel).addValue("negotiable", negotiable)
                        .addValue("status", requestedStatus).addValue("id", current.id())
                        .addValue("sellerUserId", current.sellerUserId()).addValue("version", current.version()));
    }

    public int softDelete(long id, long sellerUserId, int version) {
        return db.update("update secondhand_product set deleted=1,version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and seller_user_id=:seller and version=:version and status in (1,2)",
                Map.of("id", id, "seller", sellerUserId, "version", version));
    }

    public int changeStatus(long id, long sellerUserId, int expectedVersion, int status) {
        String riskClause = status == SecondhandProduct.ON_SHELF ? " and risk_status='APPROVED'" : "";
        return db.update("update secondhand_product set status=:status,version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and seller_user_id=:seller and version=:version and deleted=0 "
                        + "and status in (1,2)" + riskClause,
                Map.of("status", status, "id", id, "seller", sellerUserId, "version", expectedVersion));
    }

    public int compareAndSetStatus(long id, int expectedStatus, int targetStatus) {
        return db.update("update secondhand_product set status=:target,version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and status=:expected and deleted=0",
                Map.of("target", targetStatus, "id", id, "expected", expectedStatus));
    }

    public int markRiskDecision(long id, String riskStatus) {
        return db.update("update secondhand_product set risk_status=:risk,"
                        + "status=case when :risk='APPROVED' then status else 2 end,"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and deleted=0",
                Map.of("risk", riskStatus, "id", id));
    }

    public Optional<CategoryProjection> findCategory(int categoryId, int subCategoryId) {
        return db.query("select * from category_projection where category_id=:category and sub_category_id=:sub and active=1",
                Map.of("category", categoryId, "sub", subCategoryId), (rs, row) -> new CategoryProjection(
                        rs.getInt("category_id"), rs.getInt("sub_category_id"),
                        rs.getString("category_name"), rs.getString("sub_category_name")))
                .stream().findFirst();
    }

    public long countCompletedForSeller(long sellerUserId) {
        Long count = db.queryForObject("select count(*) from secondhand_product where seller_user_id=:seller and status=3",
                Map.of("seller", sellerUserId), Long.class);
        return count == null ? 0 : count;
    }

    private ProductPage page(String where, Map<String, Object> params, String order, long pageNum, long pageSize) {
        Long count = db.queryForObject("select count(*) from secondhand_product" + where, params, Long.class);
        Map<String, Object> pageParams = new HashMap<>(params);
        pageParams.put("limit", pageSize);
        pageParams.put("offset", (pageNum - 1) * pageSize);
        List<SecondhandProduct> products = db.query("select * from secondhand_product" + where + order
                        + " limit :limit offset :offset", pageParams, PRODUCT_MAPPER);
        return new ProductPage(count == null ? 0 : count, products);
    }

    private void appendFilters(StringBuilder where, Map<String, Object> params, String keyword,
            Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String conditionLevel,
            Integer negotiable) {
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (lower(name) like :keyword or lower(description) like :keyword)");
            params.put("keyword", "%" + keyword.trim().toLowerCase() + "%");
        }
        if (categoryId != null) {
            where.append(" and category_id=:categoryId");
            params.put("categoryId", categoryId);
        }
        if (minPrice != null) {
            where.append(" and sale_price>=:minPrice");
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            where.append(" and sale_price<=:maxPrice");
            params.put("maxPrice", maxPrice);
        }
        if (conditionLevel != null && !conditionLevel.isBlank()) {
            where.append(" and condition_level=:conditionLevel");
            params.put("conditionLevel", conditionLevel.trim());
        }
        if (negotiable != null) {
            where.append(" and is_negotiable=:negotiable");
            params.put("negotiable", negotiable);
        }
    }

    private static SecondhandProduct mapProduct(ResultSet rs, int row) throws SQLException {
        return new SecondhandProduct(rs.getLong("id"), rs.getLong("seller_user_id"),
                rs.getString("seller_name_snapshot"), rs.getString("name"), rs.getString("cover"),
                rs.getString("images"), rs.getString("description"), rs.getBigDecimal("origin_price"),
                rs.getBigDecimal("sale_price"), rs.getInt("category_id"), rs.getInt("sub_category_id"),
                rs.getString("condition_level"), rs.getInt("is_negotiable"), rs.getInt("status"),
                rs.getString("risk_status"), rs.getInt("version"), rs.getInt("deleted") == 1,
                localDateTime(rs, "create_time"), localDateTime(rs, "update_time"));
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public record ProductPage(long total, List<SecondhandProduct> products) {
    }

    public record CategoryProjection(int categoryId, int subCategoryId, String categoryName,
            String subCategoryName) {
    }
}
