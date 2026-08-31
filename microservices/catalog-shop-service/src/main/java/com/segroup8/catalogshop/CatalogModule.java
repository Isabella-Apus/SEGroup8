package com.segroup8.catalogshop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CatalogController {
    private final CatalogModule service;
    CatalogController(CatalogModule service) { this.service=service; }

    @GetMapping("/api/category/tree") List<CategoryNode> categories() { return service.categories(); }
    @GetMapping({"/api/product/list","/api/product/search"})
    List<Product> search(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) Long shopId,@RequestParam(required=false) BigDecimal minPrice,
            @RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="newest") String sort) {
        return service.search(keyword,categoryId,shopId,minPrice,maxPrice,sort);
    }
    @GetMapping("/api/product/detail/{productId}") Product detail(@PathVariable long productId) { return service.publicDetail(productId); }
    @GetMapping("/api/product/seller/list") List<Product> mine(@RequestHeader("X-Seller-Id") long sellerId) { return service.sellerProducts(sellerId); }
    @GetMapping("/api/product/seller/{id}") Product sellerDetail(@RequestHeader("X-Seller-Id") long sellerId,@PathVariable long id) { return service.sellerDetail(sellerId,id); }
    @PostMapping("/api/product/seller") @ResponseStatus(HttpStatus.CREATED)
    Product create(@RequestHeader("X-Seller-Id") long sellerId,@Valid @RequestBody ProductRequest r) { return service.create(sellerId,r); }
    @PutMapping("/api/product/seller/{id}") Product update(@RequestHeader("X-Seller-Id") long sellerId,@PathVariable long id,@Valid @RequestBody ProductRequest r) { return service.update(sellerId,id,r); }
    @DeleteMapping("/api/product/seller/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@RequestHeader("X-Seller-Id") long sellerId,@PathVariable long id) { service.offShelf(sellerId,id); }
    @GetMapping("/internal/products/{id}/snapshot") ProductSnapshot snapshot(@RequestHeader("X-Internal-Service-Token") String token,@PathVariable long id) { return service.snapshot(token,id); }

    record ProductRequest(@NotNull @Min(1) Long shopId,@NotNull @Min(1) Long categoryId,
            @NotBlank @Size(max=120) String name,@Size(max=2000) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,@Min(0) int stock) {}
}

@Service
class CatalogModule {
    private final JdbcClient db; private final SimpleJdbcInsert productInsert; private final RiskModule risk; private final InternalTokenPolicy tokens;
    CatalogModule(JdbcClient db,DataSource ds,RiskModule risk,InternalTokenPolicy tokens) {
        this.db=db;this.risk=risk;this.tokens=tokens;
        this.productInsert=new SimpleJdbcInsert(ds).withTableName("product").usingGeneratedKeyColumns("id");
    }
    List<CategoryNode> categories() {
        List<CategoryRow> rows=db.sql("select id,parent_id,name,sort_order from category where active=true order by sort_order,id").query(CategoryRow.class).list();
        return rows.stream().filter(r->r.parentId()==null).map(r->new CategoryNode(r.id(),r.name(),children(rows,r.id()))).toList();
    }
    private List<CategoryNode> children(List<CategoryRow> all,long parent) { return all.stream().filter(r->r.parentId()!=null&&r.parentId()==parent).map(r->new CategoryNode(r.id(),r.name(),children(all,r.id()))).toList(); }
    List<Product> search(String keyword,Long categoryId,Long shopId,BigDecimal min,BigDecimal max,String sort) {
        String q=keyword==null?"":keyword.trim().toLowerCase(Locale.ROOT);
        Comparator<Product> comparator=switch(sort==null?"newest":sort){case "priceAsc"->Comparator.comparing(Product::price);case "priceDesc"->Comparator.comparing(Product::price).reversed();default->Comparator.comparing(Product::updatedAt).reversed();};
        List<Product> result=db.sql("select * from product where status='ON_SALE'").query(Product.class).list().stream()
                .filter(p->q.isBlank()||p.name().toLowerCase(Locale.ROOT).contains(q)||p.description().toLowerCase(Locale.ROOT).contains(q))
                .filter(p->categoryId==null||p.categoryId()==categoryId).filter(p->shopId==null||p.shopId()==shopId)
                .filter(p->min==null||p.price().compareTo(min)>=0).filter(p->max==null||p.price().compareTo(max)<=0).sorted(comparator).toList();
        return result;
    }
    Product publicDetail(long id) { return db.sql("select * from product where id=:id and status='ON_SALE'").param("id",id).query(Product.class).optional().orElseThrow(()->new ApiException("PRODUCT_NOT_FOUND","在售商品不存在",HttpStatus.NOT_FOUND)); }
    List<Product> sellerProducts(long sellerId) { return db.sql("select * from product where seller_id=:seller order by updated_at desc").param("seller",sellerId).query(Product.class).list(); }
    Product sellerDetail(long sellerId,long id) { return owned(sellerId,id); }
    @Transactional Product create(long sellerId,CatalogController.ProductRequest r) {
        verifyShop(sellerId,r.shopId()); verifyCategory(r.categoryId());
        var now=java.sql.Timestamp.from(Instant.now());
        long id=productInsert.executeAndReturnKey(Map.of("seller_id",sellerId,"shop_id",r.shopId(),"category_id",r.categoryId(),"name",r.name().trim(),"description",safe(r.description()),"price",r.price(),"stock",r.stock(),"reserved_stock",0,"status","PENDING_REVIEW","updated_at",now)).longValue();
        risk.submit(id,r.name(),safe(r.description()));
        return owned(sellerId,id);
    }
    @Transactional Product update(long sellerId,long id,CatalogController.ProductRequest r) {
        Product old=owned(sellerId,id); if(old.reservedStock()>r.stock()) throw new ApiException("STOCK_BELOW_RESERVED","库存不能低于已预留数量");
        verifyShop(sellerId,r.shopId());verifyCategory(r.categoryId());
        db.sql("update product set shop_id=:shop,category_id=:category,name=:name,description=:description,price=:price,stock=:stock,status='PENDING_REVIEW',updated_at=CURRENT_TIMESTAMP where id=:id")
                .params(Map.of("shop",r.shopId(),"category",r.categoryId(),"name",r.name().trim(),"description",safe(r.description()),"price",r.price(),"stock",r.stock(),"id",id)).update();
        risk.submit(id,r.name(),safe(r.description())); return owned(sellerId,id);
    }
    void offShelf(long sellerId,long id) { owned(sellerId,id);db.sql("update product set status='OFF_SHELF',updated_at=CURRENT_TIMESTAMP where id=:id").param("id",id).update(); }
    ProductSnapshot snapshot(String token,long id) { tokens.require(token);Product p=publicDetail(id);return new ProductSnapshot(p.id(),p.name(),p.price(),p.shopId(),p.stock()-p.reservedStock()); }
    void applyRiskDecision(long productId,boolean approved) { db.sql("update product set status=:status,updated_at=CURRENT_TIMESTAMP where id=:id and status='PENDING_REVIEW'").params(Map.of("status",approved?"ON_SALE":"REJECTED","id",productId)).update(); }
    private Product owned(long seller,long id) { return db.sql("select * from product where id=:id and seller_id=:seller").params(Map.of("id",id,"seller",seller)).query(Product.class).optional().orElseThrow(()->new ApiException("PRODUCT_NOT_FOUND","商品不存在或不属于当前卖家",HttpStatus.NOT_FOUND)); }
    private void verifyShop(long seller,long shop) { if(db.sql("select count(*) from shop where id=:id and seller_id=:seller").params(Map.of("id",shop,"seller",seller)).query(Integer.class).single()==0) throw new ApiException("SHOP_OWNERSHIP_REQUIRED","只能在自己的店铺发布商品",HttpStatus.FORBIDDEN); }
    private void verifyCategory(long id) { if(db.sql("select count(*) from category where id=:id and active=true").param("id",id).query(Integer.class).single()==0) throw new ApiException("CATEGORY_NOT_FOUND","分类不存在",HttpStatus.BAD_REQUEST); }
    private String safe(String value){return value==null?"":value;}
}

record CategoryRow(long id,Long parentId,String name,int sortOrder) {}
record CategoryNode(long id,String name,List<CategoryNode> children) {}
record Product(long id,long sellerId,long shopId,long categoryId,String name,String description,BigDecimal price,int stock,int reservedStock,String status,Instant updatedAt) {}
record ProductSnapshot(long productId,String name,BigDecimal unitPrice,long shopId,int availableStock) {}
