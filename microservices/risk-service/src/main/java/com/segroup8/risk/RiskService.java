package com.segroup8.risk;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RiskService {
    private final JdbcClient db; private final RestClient catalog; private final SimpleJdbcInsert auditInsert;
    public RiskService(JdbcClient db,DataSource dataSource,RestClient.Builder builder,@Value("${clients.catalog-base-url:http://catalog-service:8081}") String url){this.db=db;this.catalog=builder.baseUrl(url).build();this.auditInsert=new SimpleJdbcInsert(dataSource).withTableName("risk_audits").usingGeneratedKeyColumns("id");}
    public Audit create(CreateCommand c){
        RuleResult result=RiskRule.evaluate(c.name()+" "+safe(c.description()));
        var now=java.sql.Timestamp.from(Instant.now());
        long id=auditInsert.executeAndReturnKey(Map.of("product_id",c.productId(),"snapshot_name",c.name(),
                "snapshot_description",safe(c.description()),"risk_level",result.level(),"rule_hits",String.join(",",result.hits()),
                "status","PENDING","decision_reason","","created_at",now,"updated_at",now)).longValue();
        return byId(id);
    }
    public List<Audit> list(String status){return (status==null||status.isBlank()
            ? db.sql("select * from risk_audits order by created_at desc")
            : db.sql("select * from risk_audits where status=:status order by created_at desc").param("status",status))
            .query(Audit.class).list();}
    public Audit decide(long id,long adminId,DecisionCommand c){
        Audit current=byId(id); if(!"PENDING".equals(current.status())) throw new RiskException("ALREADY_DECIDED","审核单已处理");
        if(!c.approved()&&(c.reason()==null||c.reason().isBlank())) throw new RiskException("REASON_REQUIRED","驳回必须填写原因");
        db.sql("update risk_audits set status=:status,decision_reason=:reason,admin_id=:admin,updated_at=CURRENT_TIMESTAMP where id=:id")
                .params(Map.of("status",c.approved()?"APPROVED":"REJECTED","reason",safe(c.reason()),"admin",adminId,"id",id)).update();
        callback(current.productId(),c.approved(),id); return byId(id);
    }
    private void callback(long productId,boolean approved,long auditId){
        try{catalog.post().uri("/api/catalog/internal/products/{id}/risk-decision",productId).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("approved",approved)).retrieve().toBodilessEntity();}
        catch(RuntimeException ex){db.sql("insert into integration_outbox(event_type,aggregate_id,payload,status,created_at) values('RISK_DECISION_MADE',:id,:payload,'PENDING',CURRENT_TIMESTAMP)")
                .params(Map.of("id",auditId,"payload","{\"productId\":"+productId+",\"approved\":"+approved+"}")).update();}
    }
    private Audit byId(long id){return db.sql("select * from risk_audits where id=:id").param("id",id).query(Audit.class).optional().orElseThrow(()->new RiskException("AUDIT_NOT_FOUND","审核单不存在"));}
    private String safe(String v){return v==null?"":v;}
    public record Audit(long id,long productId,String snapshotName,String snapshotDescription,String riskLevel,String ruleHits,String status,String decisionReason,Long adminId,Instant createdAt,Instant updatedAt){}
    public record CreateCommand(long productId,String name,String description){}
    public record DecisionCommand(boolean approved,String reason){}
}
record RuleResult(String level,List<String> hits){}
final class RiskRule {
    private static final List<String> FORBIDDEN=List.of("违禁","枪支","毒品","假证","赌博");
    static RuleResult evaluate(String text){String normalized=text.toLowerCase(Locale.ROOT);List<String> hits=FORBIDDEN.stream().filter(normalized::contains).toList();return new RuleResult(hits.isEmpty()?"LOW":"HIGH",hits);}
    private RiskRule(){}
}
class RiskException extends RuntimeException{final String code;RiskException(String code,String message){super(message);this.code=code;}}
