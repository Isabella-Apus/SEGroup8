package com.segroup8.catalogshop;

import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CatalogController {
    private final CatalogModule service; private final BehaviorModule behavior;
    CatalogController(CatalogModule service,BehaviorModule behavior) { this.service=service;this.behavior=behavior; }

    @GetMapping("/api/category/tree") ApiResult<List<CategoryNode>> categories() { return ApiResult.success(service.categories()); }
    @GetMapping("/api/product/list")
    ApiResult<PageResult<Product>> list(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) Long shopId,@RequestParam(required=false) BigDecimal minPrice,
            @RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="newest") String sort,
            @RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize,HttpServletRequest request) {
        recordSearch(request,keyword);
        return ApiResult.success(service.searchPage(keyword,categoryId,shopId,minPrice,maxPrice,sort,pageNum,pageSize));
    }
    @GetMapping("/api/product/search")
    ApiResult<PageResult<Product>> search(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) Long shopId,@RequestParam(required=false) BigDecimal minPrice,
            @RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="newest") String sort,
            @RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize,HttpServletRequest request) {
        recordSearch(request,keyword);
        return ApiResult.success(service.searchPage(keyword,categoryId,shopId,minPrice,maxPrice,sort,pageNum,pageSize));
    }
    @GetMapping("/api/product/detail/{productId}") ApiResult<Product> detail(@PathVariable long productId) { return ApiResult.success(service.publicDetail(productId)); }
    @GetMapping("/api/product/seller/list") ApiResult<PageResult<Product>> mine(HttpServletRequest request,@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize,@RequestParam(required=false) String keyword) { long seller=AuthenticationSupport.require(request).requireRole("SELLER","OFFICIAL_SELLER").userId();return ApiResult.success(service.sellerProducts(seller,keyword,pageNum,pageSize)); }
    @GetMapping("/api/product/seller/{id}") ApiResult<Product> sellerDetail(HttpServletRequest request,@PathVariable long id) { long seller=AuthenticationSupport.require(request).requireRole("SELLER","OFFICIAL_SELLER").userId();return ApiResult.success(service.sellerDetail(seller,id)); }
    @PostMapping("/api/product/seller") @ResponseStatus(HttpStatus.CREATED)
    ApiResult<Product> create(HttpServletRequest request,@Valid @RequestBody ProductRequest r) { long seller=AuthenticationSupport.require(request).requireRole("SELLER","OFFICIAL_SELLER").userId();return ApiResult.success(service.create(seller,r)); }
    @PutMapping("/api/product/seller/{id}") ApiResult<Product> update(HttpServletRequest request,@PathVariable long id,@Valid @RequestBody ProductRequest r) { long seller=AuthenticationSupport.require(request).requireRole("SELLER","OFFICIAL_SELLER").userId();return ApiResult.success(service.update(seller,id,r)); }
    @DeleteMapping("/api/product/seller/{id}") ApiResult<Void> delete(HttpServletRequest request,@PathVariable long id) { long seller=AuthenticationSupport.require(request).requireRole("SELLER","OFFICIAL_SELLER").userId();service.delete(seller,id);return ApiResult.success(null); }
    @PostMapping("/api/product/seller/{id}/status") ApiResult<Product> status(HttpServletRequest request,@PathVariable long id,@RequestBody StatusRequest r){long seller=AuthenticationSupport.require(request).requireRole("SELLER","OFFICIAL_SELLER").userId();return ApiResult.success(service.status(seller,id,r.status()));}
    @PostMapping("/api/product/seller/{id}/stock/adjust") ApiResult<Product> stock(HttpServletRequest request,@PathVariable long id,@RequestBody StockRequest r){long seller=AuthenticationSupport.require(request).requireRole("SELLER","OFFICIAL_SELLER").userId();return ApiResult.success(service.adjustStock(seller,id,r.delta()));}
    @GetMapping("/internal/products/{id}/snapshot") ApiResult<ProductSnapshot> snapshot(@org.springframework.web.bind.annotation.RequestHeader("X-Internal-Service-Token") String token,@PathVariable long id) { return ApiResult.success(service.snapshot(token,id)); }

    record ProductRequest(@Min(1) Long shopId,@Min(1) Long categoryId,@Min(1) Long subCategoryId,
            @NotBlank @Size(max=120) String name,@Size(max=2000) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,@Min(0) int stock,List<String> images,String cover,Integer status) {}
    record StatusRequest(int status){}
    record StockRequest(int delta){}
    private void recordSearch(HttpServletRequest request,String keyword){Object user=request.getAttribute(AuthenticationSupport.ATTRIBUTE);if(user instanceof AuthenticatedUser authenticated&&keyword!=null&&!keyword.isBlank())behavior.recordSearch(authenticated.userId(),keyword);}
}

@Service
class CatalogModule {
    private final JdbcClient db; private final SimpleJdbcInsert productInsert; private final RiskModule risk; private final InternalTokenPolicy tokens; private final ObjectMapper json;
    CatalogModule(JdbcClient db,DataSource ds,RiskModule risk,InternalTokenPolicy tokens,ObjectMapper json) {
        this.db=db;this.risk=risk;this.tokens=tokens;this.json=json;
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
                .filter(p->categoryId==null||p.categoryId()==categoryId||(p.subCategoryId()!=null&&p.subCategoryId()==categoryId)).filter(p->shopId==null||p.shopId()==shopId)
                .filter(p->min==null||p.price().compareTo(min)>=0).filter(p->max==null||p.price().compareTo(max)<=0).sorted(comparator).toList();
        return result;
    }
    PageResult<Product> searchPage(String keyword,Long categoryId,Long shopId,BigDecimal min,BigDecimal max,String sort,int page,int size){return page(search(keyword,categoryId,shopId,min,max,sort),page,size);}
    Product publicDetail(long id) { return db.sql("select * from product where id=:id and status='ON_SALE'").param("id",id).query(Product.class).optional().orElseThrow(()->new ApiException("PRODUCT_NOT_FOUND","在售商品不存在",HttpStatus.NOT_FOUND)); }
    PageResult<Product> sellerProducts(long sellerId,String keyword,int page,int size) {String q=keyword==null?"":keyword.toLowerCase(Locale.ROOT);List<Product> all=db.sql("select * from product where seller_id=:seller order by updated_at desc").param("seller",sellerId).query(Product.class).list().stream().filter(p->q.isBlank()||p.name().toLowerCase(Locale.ROOT).contains(q)).toList();return page(all,page,size);}
    Product sellerDetail(long sellerId,long id) { return owned(sellerId,id); }
    @Transactional Product create(long sellerId,CatalogController.ProductRequest r) {
        long shop=resolveShop(sellerId,r.shopId());long category=resolveCategory(r);Long sub=resolveSubCategory(r);verifyShop(sellerId,shop);verifyCategory(category);if(sub!=null)verifyCategory(sub);
        var now=java.sql.Timestamp.from(Instant.now());
        var values=new java.util.HashMap<String,Object>();values.put("seller_id",sellerId);values.put("shop_id",shop);values.put("category_id",category);values.put("sub_category_id",sub);values.put("name",r.name().trim());values.put("description",safe(r.description()));values.put("price",r.price());values.put("stock",r.stock());values.put("reserved_stock",0);values.put("status","PENDING_REVIEW");values.put("cover",safe(r.cover()));values.put("images",imagesJson(r.images()));values.put("updated_at",now);long id=productInsert.executeAndReturnKey(values).longValue();
        risk.submit(id,r.name(),safe(r.description()));
        return owned(sellerId,id);
    }
    @Transactional Product update(long sellerId,long id,CatalogController.ProductRequest r) {
        Product old=owned(sellerId,id); if(old.reservedStock()>r.stock()) throw new ApiException("STOCK_BELOW_RESERVED","库存不能低于已预留数量");
        long shop=resolveShop(sellerId,r.shopId());long category=resolveCategory(r);Long sub=resolveSubCategory(r);verifyShop(sellerId,shop);verifyCategory(category);if(sub!=null)verifyCategory(sub);
        db.sql("update product set shop_id=:shop,category_id=:category,sub_category_id=:sub,name=:name,description=:description,price=:price,stock=:stock,cover=:cover,images=:images,status='PENDING_REVIEW',updated_at=CURRENT_TIMESTAMP where id=:id")
                .params(Map.of("shop",shop,"category",category,"sub",sub,"name",r.name().trim(),"description",safe(r.description()),"price",r.price(),"stock",r.stock(),"cover",safe(r.cover()),"images",imagesJson(r.images()),"id",id)).update();
        risk.submit(id,r.name(),safe(r.description())); return owned(sellerId,id);
    }
    void delete(long sellerId,long id) { Product p=owned(sellerId,id);if(p.reservedStock()>0)throw new ApiException("PRODUCT_RESERVED","存在库存预留，不能删除");db.sql("delete from product_risk_audit where product_id=:id").param("id",id).update();db.sql("delete from product where id=:id").param("id",id).update(); }
    Product status(long sellerId,long id,int status){owned(sellerId,id);db.sql("update product set status=:status,updated_at=CURRENT_TIMESTAMP where id=:id").params(Map.of("status",status==1?"ON_SALE":"OFF_SHELF","id",id)).update();return owned(sellerId,id);}
    Product adjustStock(long sellerId,long id,int delta){Product p=owned(sellerId,id);if(delta==0||p.stock()+delta<p.reservedStock())throw new ApiException("INVALID_STOCK_DELTA","库存调整结果非法",HttpStatus.BAD_REQUEST);db.sql("update product set stock=stock+:delta,updated_at=CURRENT_TIMESTAMP where id=:id").params(Map.of("delta",delta,"id",id)).update();return owned(sellerId,id);}
    ProductSnapshot snapshot(String token,long id) { tokens.require(token);Product p=publicDetail(id);return new ProductSnapshot(p.id(),p.name(),p.price(),p.shopId(),p.stock()-p.reservedStock()); }
    void applyRiskDecision(long productId,boolean approved) { db.sql("update product set status=:status,updated_at=CURRENT_TIMESTAMP where id=:id and status='PENDING_REVIEW'").params(Map.of("status",approved?"ON_SALE":"REJECTED","id",productId)).update(); }
    private Product owned(long seller,long id) { return db.sql("select * from product where id=:id and seller_id=:seller").params(Map.of("id",id,"seller",seller)).query(Product.class).optional().orElseThrow(()->new ApiException("PRODUCT_NOT_FOUND","商品不存在或不属于当前卖家",HttpStatus.NOT_FOUND)); }
    private void verifyShop(long seller,long shop) { if(db.sql("select count(*) from shop where id=:id and seller_id=:seller").params(Map.of("id",shop,"seller",seller)).query(Integer.class).single()==0) throw new ApiException("SHOP_OWNERSHIP_REQUIRED","只能在自己的店铺发布商品",HttpStatus.FORBIDDEN); }
    private void verifyCategory(long id) { if(db.sql("select count(*) from category where id=:id and active=true").param("id",id).query(Integer.class).single()==0) throw new ApiException("CATEGORY_NOT_FOUND","分类不存在",HttpStatus.BAD_REQUEST); }
    private long resolveShop(long seller,Long requested){if(requested!=null)return requested;return db.sql("select id from shop where seller_id=:seller").param("seller",seller).query(Long.class).optional().orElseThrow(()->new ApiException("SHOP_NOT_FOUND","当前卖家没有店铺",HttpStatus.BAD_REQUEST));}
    private long resolveCategory(CatalogController.ProductRequest r){Long value=r.categoryId()!=null?r.categoryId():r.subCategoryId();if(value==null)throw new ApiException("CATEGORY_REQUIRED","商品分类必填",HttpStatus.BAD_REQUEST);return value;}
    private Long resolveSubCategory(CatalogController.ProductRequest r){return r.subCategoryId()!=null?r.subCategoryId():r.categoryId();}
    private <T> PageResult<T> page(List<T> all,int page,int size){if(page<1||size<1||size>100)throw new ApiException("INVALID_PAGE","pageNum 必须大于 0 且 pageSize 为 1-100",HttpStatus.BAD_REQUEST);int from=Math.min((page-1)*size,all.size());int to=Math.min(from+size,all.size());return new PageResult<>(all.size(),page,size,all.subList(from,to));}
    private String safe(String value){return value==null?"":value;}
    private String imagesJson(List<String> images){try{return json.writeValueAsString(images==null?List.of():images);}catch(Exception e){throw new ApiException("INVALID_IMAGES","商品图片格式非法",HttpStatus.BAD_REQUEST);}}
}

record CategoryRow(long id,Long parentId,String name,int sortOrder) {}
record CategoryNode(long id,String name,List<CategoryNode> children) {}
record Product(long id,long sellerId,long shopId,long categoryId,Long subCategoryId,String name,String description,BigDecimal price,int stock,int reservedStock,String cover,@JsonIgnore String images,@JsonIgnore String status,Instant updatedAt) {
    @JsonProperty("status") int statusCode(){return "ON_SALE".equals(status)?1:0;}
    @JsonProperty String statusName(){return switch(status){case "ON_SALE"->"在售";case "PENDING_REVIEW"->"待审核";case "REJECTED"->"审核驳回";default->"已下架";};}
    @JsonProperty("images") List<String> imageList(){
        if(images==null||images.isBlank()) return cover==null||cover.isBlank()?List.of():List.of(cover);
        try{return new ObjectMapper().readValue(images,new TypeReference<List<String>>(){});}catch(Exception ignored){return cover==null||cover.isBlank()?List.of():List.of(cover);}
    }
}
record ProductSnapshot(long productId,String name,BigDecimal unitPrice,long shopId,int availableStock) {}
record PageResult<T>(int total,int pageNum,int pageSize,List<T> records){}
