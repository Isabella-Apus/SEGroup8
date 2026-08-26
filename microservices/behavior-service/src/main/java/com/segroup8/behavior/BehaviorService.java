package com.segroup8.behavior;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehaviorService {
    private final JdbcClient db;public BehaviorService(JdbcClient db){this.db=db;}
    public void recordBrowse(long userId,BrowseCommand c){
        db.sql("delete from browse_history where user_id=:user and product_id=:product and product_type=:type")
                .params(Map.of("user",userId,"product",c.productId(),"type",c.productType())).update();
        db.sql("insert into browse_history(user_id,product_id,product_type,browsed_at) values(:user,:product,:type,CURRENT_TIMESTAMP)")
                .params(Map.of("user",userId,"product",c.productId(),"type",c.productType())).update();
    }
    public List<Browse> browse(long userId){return db.sql("select * from browse_history where user_id=:user order by browsed_at desc limit 100").param("user",userId).query(Browse.class).list();}
    public void deleteBrowse(long userId,long id){int changed=db.sql("delete from browse_history where id=:id and user_id=:user").params(Map.of("id",id,"user",userId)).update();if(changed==0)throw new BehaviorException("HISTORY_NOT_FOUND","浏览记录不存在");}
    public void clearBrowse(long userId){db.sql("delete from browse_history where user_id=:user").param("user",userId).update();}
    @Transactional public void recordSearch(long userId,String raw){
        String keyword=KeywordPolicy.normalize(raw);
        db.sql("insert into search_history(user_id,keyword,searched_at) values(:user,:keyword,CURRENT_TIMESTAMP)").params(Map.of("user",userId,"keyword",keyword)).update();
        Integer count=db.sql("select search_count from keyword_stats where keyword=:keyword").param("keyword",keyword).query(Integer.class).optional().orElse(null);
        if(count==null)db.sql("insert into keyword_stats(keyword,search_count,updated_at) values(:keyword,1,CURRENT_TIMESTAMP)").param("keyword",keyword).update();
        else db.sql("update keyword_stats set search_count=:count,updated_at=CURRENT_TIMESTAMP where keyword=:keyword").params(Map.of("count",count+1,"keyword",keyword)).update();
    }
    public List<Search> searches(long userId){return db.sql("select * from search_history where user_id=:user order by searched_at desc limit 20").param("user",userId).query(Search.class).list();}
    public List<HotKeyword> hot(){return db.sql("select keyword,search_count from keyword_stats order by search_count desc,keyword asc limit 10").query(HotKeyword.class).list();}
    public record Browse(long id,long userId,long productId,String productType,Instant browsedAt){}
    public record Search(long id,long userId,String keyword,Instant searchedAt){}
    public record HotKeyword(String keyword,int searchCount){}
    public record BrowseCommand(long productId,String productType){}
}
final class KeywordPolicy{
    static String normalize(String raw){if(raw==null)throw new BehaviorException("INVALID_KEYWORD","关键词不能为空");String value=raw.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);if(value.isBlank()||value.length()>64)throw new BehaviorException("INVALID_KEYWORD","关键词长度必须为 1-64");return value;}
    private KeywordPolicy(){}
}
class BehaviorException extends RuntimeException{final String code;BehaviorException(String code,String message){super(message);this.code=code;}}
