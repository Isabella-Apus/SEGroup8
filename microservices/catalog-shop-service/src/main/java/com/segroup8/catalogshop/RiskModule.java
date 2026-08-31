package com.segroup8.catalogshop;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
    @GetMapping ApiResult<PageResult<RiskAuditView>> list(HttpServletRequest request,@RequestParam(required=false) String status,@RequestParam(required=false) String auditStatus,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="1") int pageNum,@RequestParam(defaultValue="20") int pageSize){AuthenticationSupport.require(request).requireRole("ADMIN");return ApiResult.success(service.listView(auditStatus==null?status:auditStatus,keyword,pageNum,pageSize));}
    @GetMapping("/{id}") ApiResult<RiskAuditView> detail(HttpServletRequest request,@PathVariable long id){AuthenticationSupport.require(request).requireRole("ADMIN");return ApiResult.success(service.view(id));}
    @PostMapping("/{id}/decision") ApiResult<RiskAuditView> decide(HttpServletRequest request,@PathVariable long id,@Valid @RequestBody Decision r){long admin=AuthenticationSupport.require(request).requireRole("ADMIN").userId();return ApiResult.success(service.view(service.decide(id,admin,r).id()));}
    record Decision(Boolean approved,String reason,String decision,String adminRemark){boolean isApproved(){return approved!=null?approved:"APPROVED".equalsIgnoreCase(decision);}String actualReason(){return reason!=null?reason:adminRemark;}}
}

@Service
class RiskModule {
    private static final List<String> FORBIDDEN=List.of("违禁","枪支","毒品","假证","赌博","高仿","假冒");
    private final JdbcClient db;private final SimpleJdbcInsert insert;private final boolean llmConfigured;
    RiskModule(JdbcClient db,DataSource ds,@Value("${catalog-shop.risk-llm-api-key:}") String key){this.db=db;this.insert=new SimpleJdbcInsert(ds).withTableName("product_risk_audit").usingGeneratedKeyColumns("id");this.llmConfigured=key!=null&&!key.isBlank();}
    void submit(long productId,String name,String description){String text=(name+" "+description).toLowerCase(Locale.ROOT);List<String> hits=FORBIDDEN.stream().filter(text::contains).toList();String level=hits.isEmpty()?"LOW":"HIGH";String status=hits.isEmpty()?"APPROVED":"PENDING";String reason=hits.isEmpty()?"DETERMINISTIC_RULES_AUTO_APPROVED":(llmConfigured?"RULES_AND_LLM":"DETERMINISTIC_RULES_NO_LLM_KEY");insert.execute(Map.of("product_id",productId,"snapshot_name",name,"snapshot_description",description,"risk_level",level,"rule_hits",String.join(",",hits),"status",status,"decision_reason",reason,"created_at",java.sql.Timestamp.from(Instant.now()),"updated_at",java.sql.Timestamp.from(Instant.now())));if(hits.isEmpty())db.sql("update product set status='ON_SALE',updated_at=CURRENT_TIMESTAMP where id=:id").param("id",productId).update();}
    List<RiskAudit> list(String status){if(status==null||status.isBlank())return db.sql("select * from product_risk_audit order by created_at desc").query(RiskAudit.class).list();if(!Set.of("PENDING","APPROVED","REJECTED").contains(status))throw new ApiException("INVALID_STATUS","非法审核状态",HttpStatus.BAD_REQUEST);return db.sql("select * from product_risk_audit where status=:status order by created_at desc").param("status",status).query(RiskAudit.class).list();}
    PageResult<RiskAuditView> listView(String status,String keyword,int page,int size){String q=keyword==null?"":keyword.toLowerCase(Locale.ROOT);List<RiskAuditView> all=list(status).stream().map(a->view(a.id())).filter(a->q.isBlank()||a.productName().toLowerCase(Locale.ROOT).contains(q)).toList();if(page<1||size<1||size>100)throw new ApiException("INVALID_PAGE","分页参数非法",HttpStatus.BAD_REQUEST);int from=Math.min((page-1)*size,all.size()),to=Math.min(from+size,all.size());return new PageResult<>(all.size(),page,size,all.subList(from,to));}
    RiskAuditView view(long id){RiskAudit a=byId(id);return new RiskAuditView(a.id(),a.productId(),a.snapshotName(),a.riskLevel(),"HIGH".equals(a.riskLevel())?90:10,List.of(a.ruleHits()),"HIGH".equals(a.riskLevel())?"ADMIN_REVIEW":"AUTO_PASS",a.status(),a.decisionReason(),a.adminId(),a.createdAt(),a.updatedAt());}
    RiskAudit byId(long id){return db.sql("select * from product_risk_audit where id=:id").param("id",id).query(RiskAudit.class).optional().orElseThrow(()->new ApiException("AUDIT_NOT_FOUND","审核单不存在",HttpStatus.NOT_FOUND));}
    @Transactional RiskAudit decide(long id,long admin,RiskController.Decision d){RiskAudit current=byId(id);if(!"PENDING".equals(current.status()))throw new ApiException("ALREADY_DECIDED","审核单已处理");boolean approved=d.isApproved();String actualReason=d.actualReason();if(!approved&&(actualReason==null||actualReason.isBlank()))throw new ApiException("REASON_REQUIRED","驳回必须填写原因",HttpStatus.BAD_REQUEST);String status=approved?"APPROVED":"REJECTED";String reason=actualReason==null?"":actualReason;db.sql("update product_risk_audit set status=:status,decision_reason=:reason,admin_id=:admin,updated_at=CURRENT_TIMESTAMP where id=:id").params(Map.of("status",status,"reason",reason,"admin",admin,"id",id)).update();db.sql("update product set status=:status,updated_at=CURRENT_TIMESTAMP where id=:product and status='PENDING_REVIEW'").params(Map.of("status",approved?"ON_SALE":"REJECTED","product",current.productId())).update();long recipient=db.sql("select seller_id from product where id=:id").param("id",current.productId()).query(Long.class).single();String payload="{\"recipientUserId\":"+recipient+",\"displayTitle\":\"商品审核结果\",\"displayText\":\"商品 "+escape(current.snapshotName())+" 审核结果："+status+"\",\"dedupeKey\":\"risk-decision:"+id+"\",\"businessId\":\""+current.productId()+"\",\"businessType\":\"PRODUCT\",\"notificationType\":\"RISK_DECISION\",\"targetPath\":\"/merchant/seller-products\",\"scope\":\"seller\"}";db.sql("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload,status,destination,next_attempt_at) values(:event,'PRODUCT',:aggregate,'NotificationRequested.v1',:payload,'PENDING','MESSAGING',CURRENT_TIMESTAMP)").params(Map.of("event",UUID.randomUUID().toString(),"aggregate",String.valueOf(current.productId()),"payload",payload)).update();return byId(id);}
    private String escape(String value){return value.replace("\\","\\\\").replace("\"","\\\"");}
}

record RiskAudit(long id,long productId,String snapshotName,String snapshotDescription,String riskLevel,String ruleHits,String status,String decisionReason,Long adminId,Instant createdAt,Instant updatedAt){}
record RiskAuditView(long id,long productId,String productName,String riskLevel,int riskScore,List<String> riskReasons,String suggestion,String auditStatus,String adminRemark,Long adminId,Instant createdAt,Instant updatedAt){}
