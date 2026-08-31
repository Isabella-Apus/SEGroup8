package com.segroup8.catalogshop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/shop")
class ShopController {
    private final ShopModule service; ShopController(ShopModule service){this.service=service;}
    @GetMapping("/public/{id}") ShopView publicView(@PathVariable long id){return service.publicView(id);}
    @GetMapping("/seller/current") Shop mine(@RequestHeader("X-Seller-Id") long sellerId){return service.mine(sellerId);}
    @PutMapping("/seller/current/settings") Shop settings(@RequestHeader("X-Seller-Id") long sellerId,@Valid @RequestBody SettingsRequest r){return service.settings(sellerId,r);}
    @PutMapping("/seller/current/decoration") Shop decorate(@RequestHeader("X-Seller-Id") long sellerId,@Valid @RequestBody DecorationRequest r){return service.decorate(sellerId,r);}
    record SettingsRequest(@NotBlank @Size(max=80) String name,@Size(max=500) String announcement,boolean open){}
    record DecorationRequest(@NotBlank String template,@NotBlank @Size(max=20000) String contentJson){}
}

@RestController @RequestMapping("/internal/events")
class CatalogShopEventController {
    private final ShopModule shops;private final InternalTokenPolicy tokens;
    CatalogShopEventController(ShopModule shops,InternalTokenPolicy tokens){this.shops=shops;this.tokens=tokens;}
    @PostMapping("/merchant-approved") @ResponseStatus(HttpStatus.ACCEPTED)
    Shop merchantApproved(@RequestHeader("X-Internal-Service-Token") String token,@Valid @RequestBody MerchantApproved event){tokens.require(token);return shops.onMerchantApproved(event);}
    record MerchantApproved(@NotBlank String eventId,@NotBlank String applicationId,@Min(1) long sellerId,@NotBlank @Size(max=80) String shopName){}
}

@Service
class ShopModule {
    private final JdbcClient db;private final SimpleJdbcInsert insert;private final ObjectMapper json;
    ShopModule(JdbcClient db,DataSource ds,ObjectMapper json){this.db=db;this.json=json;this.insert=new SimpleJdbcInsert(ds).withTableName("shop").usingGeneratedKeyColumns("id");}
    ShopView publicView(long id){Shop shop=db.sql("select * from shop where id=:id and status='OPEN'").param("id",id).query(Shop.class).optional().orElseThrow(()->new ApiException("SHOP_NOT_FOUND","公开店铺不存在或已关闭",HttpStatus.NOT_FOUND));List<Product> products=db.sql("select * from product where shop_id=:id and status='ON_SALE' order by updated_at desc").param("id",id).query(Product.class).list();return new ShopView(shop,products);}
    Shop mine(long seller){return db.sql("select * from shop where seller_id=:seller").param("seller",seller).query(Shop.class).optional().orElseThrow(()->new ApiException("SHOP_NOT_FOUND","当前卖家没有店铺",HttpStatus.NOT_FOUND));}
    Shop settings(long seller,ShopController.SettingsRequest r){mine(seller);db.sql("update shop set name=:name,announcement=:announcement,status=:status,updated_at=CURRENT_TIMESTAMP where seller_id=:seller").params(Map.of("name",r.name().trim(),"announcement",r.announcement()==null?"":r.announcement(),"status",r.open()?"OPEN":"CLOSED","seller",seller)).update();return mine(seller);}
    Shop decorate(long seller,ShopController.DecorationRequest r){mine(seller);if(!Set.of("CLASSIC","GRID","STORY").contains(r.template()))throw new ApiException("INVALID_TEMPLATE","装修模板只允许 CLASSIC、GRID、STORY",HttpStatus.BAD_REQUEST);try{if(!json.readTree(r.contentJson()).isObject())throw new ApiException("INVALID_DECORATION","装修内容必须是 JSON 对象",HttpStatus.BAD_REQUEST);}catch(JsonProcessingException e){throw new ApiException("INVALID_DECORATION","装修内容必须是合法 JSON",HttpStatus.BAD_REQUEST);}db.sql("update shop set decoration_template=:template,decoration_json=:json,updated_at=CURRENT_TIMESTAMP where seller_id=:seller").params(Map.of("template",r.template(),"json",r.contentJson(),"seller",seller)).update();return mine(seller);}
    @Transactional Shop onMerchantApproved(CatalogShopEventController.MerchantApproved e){
        Shop existing=db.sql("select * from shop where merchant_application_id=:id").param("id",e.applicationId()).query(Shop.class).optional().orElse(null);if(existing!=null)return existing;
        if(db.sql("select count(*) from idempotency_record where scope='MerchantApproved.v1' and idempotency_key=:key").param("key",e.eventId()).query(Integer.class).single()>0)return mine(e.sellerId());
        long id=insert.executeAndReturnKey(Map.of("seller_id",e.sellerId(),"merchant_application_id",e.applicationId(),"name",e.shopName(),"announcement","","status","OPEN","decoration_template","CLASSIC","decoration_json","{}","updated_at",java.sql.Timestamp.from(Instant.now()))).longValue();
        db.sql("insert into idempotency_record(scope,idempotency_key,resource_id) values('MerchantApproved.v1',:key,:resource)").params(Map.of("key",e.eventId(),"resource",String.valueOf(id))).update();return mine(e.sellerId());
    }
}

@Component
class InternalTokenPolicy {
    private final String expected;InternalTokenPolicy(@Value("${catalog-shop.internal-token}") String expected){this.expected=expected;}
    void require(String actual){if(actual==null||!constantTime(expected,actual))throw new ApiException("INVALID_INTERNAL_TOKEN","内部服务令牌无效",HttpStatus.UNAUTHORIZED);}
    private boolean constantTime(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),b.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
}

record Shop(long id,long sellerId,String merchantApplicationId,String name,String announcement,String status,String decorationTemplate,String decorationJson,Instant updatedAt){}
record ShopView(Shop shop,List<Product> products){}
