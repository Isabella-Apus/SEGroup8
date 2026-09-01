package com.segroup8.catalogshop;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BehaviorController {
    private final BehaviorModule service;BehaviorController(BehaviorModule service){this.service=service;}
    @PostMapping("/api/user/browse-history") @ResponseStatus(HttpStatus.CREATED) ApiResult<Void> browse(HttpServletRequest request,@Valid @RequestBody BrowseRequest r){long user=AuthenticationSupport.require(request).userId();service.recordBrowse(user,r.productId(),r.productType());return ApiResult.success(null);}
    @GetMapping("/api/user/browse-history") ApiResult<List<BrowseView>> browseList(HttpServletRequest request){return ApiResult.success(service.browse(AuthenticationSupport.require(request).userId()));}
    @DeleteMapping("/api/user/browse-history/{id}") ApiResult<Void> delete(HttpServletRequest request,@PathVariable long id){service.deleteBrowse(AuthenticationSupport.require(request).userId(),id);return ApiResult.success(null);}
    @PostMapping("/api/user/browse-history/delete-batch") ApiResult<Void> deleteBatch(HttpServletRequest request,@RequestBody List<Long> ids){service.deleteBatch(AuthenticationSupport.require(request).userId(),ids);return ApiResult.success(null);}
    @DeleteMapping({"/api/user/browse-history","/api/user/browse-history/all"}) ApiResult<Void> clear(HttpServletRequest request){service.clearBrowse(AuthenticationSupport.require(request).userId());return ApiResult.success(null);}
    @PostMapping("/api/search/history") @ResponseStatus(HttpStatus.CREATED) ApiResult<Void> search(HttpServletRequest request,@Valid @RequestBody SearchRequest r){service.recordSearch(AuthenticationSupport.require(request).userId(),r.keyword());return ApiResult.success(null);}
    @GetMapping("/api/search/history") ApiResult<List<String>> searches(HttpServletRequest request){return ApiResult.success(service.searches(AuthenticationSupport.require(request).userId()).stream().map(SearchEntry::keyword).toList());}
    @DeleteMapping("/api/search/history") ApiResult<Void> clearSearches(HttpServletRequest request){service.clearSearches(AuthenticationSupport.require(request).userId());return ApiResult.success(null);}
    @GetMapping("/api/search/hot") ApiResult<List<HotKeyword>> hot(){return ApiResult.success(service.hot());}
    record BrowseRequest(@Min(1) long productId,@NotBlank String productType){}
    record SearchRequest(@NotBlank String keyword){}
}

@Service
class BehaviorModule {
    private final JdbcClient db;BehaviorModule(JdbcClient db){this.db=db;}
    void recordBrowse(long user,long product,String type){db.sql("delete from browse_history where user_id=:user and product_id=:product and product_type=:type").params(Map.of("user",user,"product",product,"type",type)).update();db.sql("insert into browse_history(user_id,product_id,product_type,browsed_at) values(:user,:product,:type,CURRENT_TIMESTAMP)").params(Map.of("user",user,"product",product,"type",type)).update();}
    List<BrowseView> browse(long user){return db.sql("select h.id,h.product_type,h.browsed_at,p.id product_id,p.name product_name,p.price,p.shop_id from browse_history h join product p on p.id=h.product_id where h.user_id=:user order by h.browsed_at desc limit 100").param("user",user).query((rs,n)->new BrowseView(rs.getLong("id"),rs.getString("product_type"),rs.getTimestamp("browsed_at").toInstant(),Map.of("id",rs.getLong("product_id"),"name",rs.getString("product_name"),"price",rs.getBigDecimal("price"),"shopId",rs.getLong("shop_id")))).list();}
    void deleteBrowse(long user,long id){if(db.sql("delete from browse_history where id=:id and user_id=:user").params(Map.of("id",id,"user",user)).update()==0)throw new ApiException("HISTORY_NOT_FOUND","浏览记录不存在",HttpStatus.NOT_FOUND);}
    void clearBrowse(long user){db.sql("delete from browse_history where user_id=:user").param("user",user).update();}
    void deleteBatch(long user,List<Long> ids){if(ids==null||ids.isEmpty())return;for(Long id:ids)db.sql("delete from browse_history where id=:id and user_id=:user").params(Map.of("id",id,"user",user)).update();}
    @Transactional void recordSearch(long user,String raw){String keyword=displayKeyword(raw);db.sql("insert into user_search_history(user_id,keyword,searched_at) values(:user,:keyword,CURRENT_TIMESTAMP)").params(Map.of("user",user,"keyword",keyword)).update();if(db.sql("update search_keyword_stat set search_count=search_count+1,updated_at=CURRENT_TIMESTAMP where keyword=:keyword").param("keyword",keyword).update()==0)db.sql("insert into search_keyword_stat(keyword,search_count,updated_at) values(:keyword,1,CURRENT_TIMESTAMP)").param("keyword",keyword).update();}
    List<SearchEntry> searches(long user){return db.sql("select * from user_search_history where user_id=:user order by searched_at desc limit 20").param("user",user).query(SearchEntry.class).list();}
    void clearSearches(long user){db.sql("delete from user_search_history where user_id=:user").param("user",user).update();}
    List<HotKeyword> hot(){return db.sql("select keyword,search_count from search_keyword_stat order by search_count desc,keyword asc limit 10").query(HotKeyword.class).list();}
    private String displayKeyword(String raw){String value=raw.trim().replaceAll("\\s+"," ");if(value.isBlank()||value.length()>64)throw new ApiException("INVALID_KEYWORD","关键词长度必须为 1-64",HttpStatus.BAD_REQUEST);return value;}
}

record BrowseEntry(long id,long userId,long productId,String productType,Instant browsedAt){}
record BrowseView(long id,String productType,Instant browseTime,Map<String,Object> product){}
record SearchEntry(long id,long userId,String keyword,Instant searchedAt){}
record HotKeyword(String keyword,int searchCount){}
