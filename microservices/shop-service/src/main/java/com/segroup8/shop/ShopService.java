package com.segroup8.shop;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ShopService {
    private final JdbcClient db;
    private final RestClient catalog;
    public ShopService(JdbcClient db, RestClient.Builder builder,
            @Value("${clients.catalog-base-url:http://catalog-service:8081}") String url) {
        this.db=db; this.catalog=builder.baseUrl(url).build();
    }
    public ShopView publicView(long id) {
        Shop shop=byId(id); List<Map<String,Object>> products; boolean available=true;
        try {
            products=catalog.get().uri(uri -> uri.path("/api/catalog/products").queryParam("shopId",id).build())
                    .retrieve().body(new ParameterizedTypeReference<>() {});
        } catch (RuntimeException ex) { products=List.of(); available=false; }
        return new ShopView(shop,products,available);
    }
    public Shop mine(long sellerId) { return db.sql("select * from shops where seller_id=:seller").param("seller",sellerId)
            .query(Shop.class).optional().orElseThrow(() -> new ShopException("SHOP_NOT_FOUND","当前卖家没有店铺")); }
    public Shop settings(long sellerId, SettingsCommand c) {
        db.sql("update shops set name=:name,announcement=:announcement,status=:status,updated_at=CURRENT_TIMESTAMP where seller_id=:seller")
                .params(Map.of("name",c.name(),"announcement",safe(c.announcement()),"status",c.open()?"OPEN":"CLOSED","seller",sellerId)).update();
        return mine(sellerId);
    }
    public Shop decorate(long sellerId, DecorationCommand c) {
        DecorationPolicy.validate(c.template(),c.contentJson());
        db.sql("update shops set decoration_template=:template,decoration_json=:json,updated_at=CURRENT_TIMESTAMP where seller_id=:seller")
                .params(Map.of("template",c.template(),"json",c.contentJson(),"seller",sellerId)).update();
        return mine(sellerId);
    }
    private Shop byId(long id) { return db.sql("select * from shops where id=:id and status='OPEN'").param("id",id).query(Shop.class)
            .optional().orElseThrow(() -> new ShopException("SHOP_NOT_FOUND","公开店铺不存在或已关闭")); }
    private String safe(String s) { return s==null?"":s; }
    public record Shop(long id,long sellerId,String name,String announcement,String status,String decorationTemplate,String decorationJson,Instant updatedAt) {}
    public record ShopView(Shop shop,List<Map<String,Object>> products,boolean catalogAvailable) {}
    public record SettingsCommand(String name,String announcement,boolean open) {}
    public record DecorationCommand(String template,String contentJson) {}
}
final class DecorationPolicy {
    private static final java.util.Set<String> ALLOWED=java.util.Set.of("CLASSIC","GRID","STORY");
    static void validate(String template,String json) {
        if(!ALLOWED.contains(template)) throw new ShopException("INVALID_TEMPLATE","装修模板只允许 CLASSIC、GRID、STORY");
        if(json==null || json.length()>20000) throw new ShopException("INVALID_DECORATION","装修内容不能为空且不能超过 20000 字符");
        try {
            if (!new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).isObject()) {
                throw new ShopException("INVALID_DECORATION","装修内容必须是 JSON 对象");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ShopException("INVALID_DECORATION","装修内容必须是合法 JSON 对象");
        }
    }
    private DecorationPolicy(){}
}
class ShopException extends RuntimeException { final String code; ShopException(String code,String message){super(message);this.code=code;} }
