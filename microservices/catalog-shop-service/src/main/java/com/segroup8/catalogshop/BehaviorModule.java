package com.segroup8.catalogshop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BehaviorController {
    private final BehaviorModule service;BehaviorController(BehaviorModule service){this.service=service;}
    @PostMapping("/api/user/browse-history") @ResponseStatus(HttpStatus.CREATED) void browse(@RequestHeader("X-User-Id") long user,@Valid @RequestBody BrowseRequest r){service.recordBrowse(user,r.productId(),r.productType());}
    @GetMapping("/api/user/browse-history") List<BrowseEntry> browseList(@RequestHeader("X-User-Id") long user){return service.browse(user);}
    @DeleteMapping("/api/user/browse-history/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@RequestHeader("X-User-Id") long user,@PathVariable long id){service.deleteBrowse(user,id);}
    @DeleteMapping("/api/user/browse-history") @ResponseStatus(HttpStatus.NO_CONTENT) void clear(@RequestHeader("X-User-Id") long user){service.clearBrowse(user);}
    @PostMapping("/api/search/history") @ResponseStatus(HttpStatus.CREATED) void search(@RequestHeader("X-User-Id") long user,@Valid @RequestBody SearchRequest r){service.recordSearch(user,r.keyword());}
    @GetMapping("/api/search/history") List<SearchEntry> searches(@RequestHeader("X-User-Id") long user){return service.searches(user);}
    @DeleteMapping("/api/search/history") @ResponseStatus(HttpStatus.NO_CONTENT) void clearSearches(@RequestHeader("X-User-Id") long user){service.clearSearches(user);}
    @GetMapping("/api/search/hot") List<HotKeyword> hot(){return service.hot();}
    record BrowseRequest(@Min(1) long productId,@NotBlank String productType){}
    record SearchRequest(@NotBlank String keyword){}
}

@Service
class BehaviorModule {
    private final JdbcClient db;BehaviorModule(JdbcClient db){this.db=db;}
    void recordBrowse(long user,long product,String type){db.sql("delete from browse_history where user_id=:user and product_id=:product and product_type=:type").params(Map.of("user",user,"product",product,"type",type)).update();db.sql("insert into browse_history(user_id,product_id,product_type,browsed_at) values(:user,:product,:type,CURRENT_TIMESTAMP)").params(Map.of("user",user,"product",product,"type",type)).update();}
    List<BrowseEntry> browse(long user){return db.sql("select * from browse_history where user_id=:user order by browsed_at desc limit 100").param("user",user).query(BrowseEntry.class).list();}
    void deleteBrowse(long user,long id){if(db.sql("delete from browse_history where id=:id and user_id=:user").params(Map.of("id",id,"user",user)).update()==0)throw new ApiException("HISTORY_NOT_FOUND","浏览记录不存在",HttpStatus.NOT_FOUND);}
    void clearBrowse(long user){db.sql("delete from browse_history where user_id=:user").param("user",user).update();}
    @Transactional void recordSearch(long user,String raw){String keyword=normalize(raw);db.sql("insert into user_search_history(user_id,keyword,searched_at) values(:user,:keyword,CURRENT_TIMESTAMP)").params(Map.of("user",user,"keyword",keyword)).update();if(db.sql("update search_keyword_stat set search_count=search_count+1,updated_at=CURRENT_TIMESTAMP where keyword=:keyword").param("keyword",keyword).update()==0)db.sql("insert into search_keyword_stat(keyword,search_count,updated_at) values(:keyword,1,CURRENT_TIMESTAMP)").param("keyword",keyword).update();}
    List<SearchEntry> searches(long user){return db.sql("select * from user_search_history where user_id=:user order by searched_at desc limit 20").param("user",user).query(SearchEntry.class).list();}
    void clearSearches(long user){db.sql("delete from user_search_history where user_id=:user").param("user",user).update();}
    List<HotKeyword> hot(){return db.sql("select keyword,search_count from search_keyword_stat order by search_count desc,keyword asc limit 10").query(HotKeyword.class).list();}
    private String normalize(String raw){String value=raw.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);if(value.isBlank()||value.length()>64)throw new ApiException("INVALID_KEYWORD","关键词长度必须为 1-64",HttpStatus.BAD_REQUEST);return value;}
}

record BrowseEntry(long id,long userId,long productId,String productType,Instant browsedAt){}
record SearchEntry(long id,long userId,String keyword,Instant searchedAt){}
record HotKeyword(String keyword,int searchCount){}
