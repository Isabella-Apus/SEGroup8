package com.segroup8.catalogshop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/admin/product-risk-audits")
class RiskController {
    private final RiskModule service;RiskController(RiskModule service){this.service=service;}
    @GetMapping List<RiskAudit> list(@RequestHeader("X-Admin-Id") long adminId,@RequestParam(required=false) String status){return service.list(status);}
    @GetMapping("/{id}") RiskAudit detail(@RequestHeader("X-Admin-Id") long adminId,@PathVariable long id){return service.byId(id);}
    @PostMapping("/{id}/decision") RiskAudit decide(@RequestHeader("X-Admin-Id") long adminId,@PathVariable long id,@Valid @RequestBody Decision r){return service.decide(id,adminId,r);}
    record Decision(boolean approved,String reason){}
}

@Service
class RiskModule {
    private static final List<String> FORBIDDEN=List.of("违禁","枪支","毒品","假证","赌博");
    private final JdbcClient db;private final SimpleJdbcInsert insert;private final boolean llmConfigured;
    RiskModule(JdbcClient db,DataSource ds,@Value("${catalog-shop.risk-llm-api-key:}") String key){this.db=db;this.insert=new SimpleJdbcInsert(ds).withTableName("product_risk_audit").usingGeneratedKeyColumns("id");this.llmConfigured=key!=null&&!key.isBlank();}
    void submit(long productId,String name,String description){String text=(name+" "+description).toLowerCase(Locale.ROOT);List<String> hits=FORBIDDEN.stream().filter(text::contains).toList();String level=hits.isEmpty()?"LOW":"HIGH";String reason=llmConfigured?"RULES_AND_LLM":"DETERMINISTIC_RULES_NO_LLM_KEY";insert.execute(Map.of("product_id",productId,"snapshot_name",name,"snapshot_description",description,"risk_level",level,"rule_hits",String.join(",",hits),"status","PENDING","decision_reason",reason,"created_at",java.sql.Timestamp.from(Instant.now()),"updated_at",java.sql.Timestamp.from(Instant.now())));}
    List<RiskAudit> list(String status){if(status==null||status.isBlank())return db.sql("select * from product_risk_audit order by created_at desc").query(RiskAudit.class).list();if(!Set.of("PENDING","APPROVED","REJECTED").contains(status))throw new ApiException("INVALID_STATUS","非法审核状态",HttpStatus.BAD_REQUEST);return db.sql("select * from product_risk_audit where status=:status order by created_at desc").param("status",status).query(RiskAudit.class).list();}
    RiskAudit byId(long id){return db.sql("select * from product_risk_audit where id=:id").param("id",id).query(RiskAudit.class).optional().orElseThrow(()->new ApiException("AUDIT_NOT_FOUND","审核单不存在",HttpStatus.NOT_FOUND));}
    @Transactional RiskAudit decide(long id,long admin,RiskController.Decision d){RiskAudit current=byId(id);if(!"PENDING".equals(current.status()))throw new ApiException("ALREADY_DECIDED","审核单已处理");if(!d.approved()&&(d.reason()==null||d.reason().isBlank()))throw new ApiException("REASON_REQUIRED","驳回必须填写原因",HttpStatus.BAD_REQUEST);String status=d.approved()?"APPROVED":"REJECTED";String reason=d.reason()==null?"":d.reason();db.sql("update product_risk_audit set status=:status,decision_reason=:reason,admin_id=:admin,updated_at=CURRENT_TIMESTAMP where id=:id").params(Map.of("status",status,"reason",reason,"admin",admin,"id",id)).update();db.sql("update product set status=:status,updated_at=CURRENT_TIMESTAMP where id=:product and status='PENDING_REVIEW'").params(Map.of("status",d.approved()?"ON_SALE":"REJECTED","product",current.productId())).update();String payload="{\"auditId\":"+id+",\"productId\":"+current.productId()+",\"status\":\""+status+"\"}";db.sql("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload,status) values(:event,'PRODUCT',:aggregate,'NotificationRequested.v1',:payload,'PENDING')").params(Map.of("event",UUID.randomUUID().toString(),"aggregate",String.valueOf(current.productId()),"payload",payload)).update();return byId(id);}
}

record RiskAudit(long id,long productId,String snapshotName,String snapshotDescription,String riskLevel,String ruleHits,String status,String decisionReason,Long adminId,Instant createdAt,Instant updatedAt){}
