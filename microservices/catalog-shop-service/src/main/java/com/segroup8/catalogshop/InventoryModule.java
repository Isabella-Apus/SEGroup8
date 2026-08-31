package com.segroup8.catalogshop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/internal/inventory/reservations")
class InventoryController {
    private final InventoryModule service;private final InternalTokenPolicy tokens;
    InventoryController(InventoryModule service,InternalTokenPolicy tokens){this.service=service;this.tokens=tokens;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    Reservation reserve(@RequestHeader("X-Internal-Service-Token") String token,@RequestHeader("X-Idempotency-Key") String key,@Valid @RequestBody ReserveRequest r){tokens.require(token);return service.reserve(key,r);}
    @GetMapping("/{id}") Reservation get(@RequestHeader("X-Internal-Service-Token") String token,@PathVariable long id){tokens.require(token);return service.get(id);}
    @PostMapping("/{id}/confirm") Reservation confirm(@RequestHeader("X-Internal-Service-Token") String token,@PathVariable long id){tokens.require(token);return service.confirm(id);}
    @PostMapping("/{id}/release") Reservation release(@RequestHeader("X-Internal-Service-Token") String token,@PathVariable long id){tokens.require(token);return service.release(id,"RELEASED");}
    record ReserveRequest(@NotBlank String orderId,@NotEmpty List<@Valid Item> items){}
    record Item(@Min(1) long productId,@Min(1) int quantity){}
}

@Service
class InventoryModule {
    private final JdbcClient db;private final SimpleJdbcInsert insert;private final long ttl;
    InventoryModule(JdbcClient db,DataSource ds,@Value("${catalog-shop.reservation-ttl-seconds:900}") long ttl){this.db=db;this.ttl=ttl;this.insert=new SimpleJdbcInsert(ds).withTableName("inventory_reservation").usingGeneratedKeyColumns("id");}
    @Transactional Reservation reserve(String key,InventoryController.ReserveRequest request){
        if(key==null||key.isBlank()||key.length()>128)throw new ApiException("IDEMPOTENCY_KEY_REQUIRED","X-Idempotency-Key 必填且最长 128 字符",HttpStatus.BAD_REQUEST);
        Reservation existing=db.sql("select * from inventory_reservation where idempotency_key=:key").param("key",key).query(ReservationRow.class).optional().map(this::assemble).orElse(null);if(existing!=null)return existing;
        Map<Long,Integer> quantities=new java.util.LinkedHashMap<>();for(var item:request.items())quantities.merge(item.productId(),item.quantity(),Integer::sum);
        for(var item:quantities.entrySet()){ProductStock p=db.sql("select id,stock,reserved_stock,status from product where id=:id for update").param("id",item.getKey()).query(ProductStock.class).optional().orElseThrow(()->new ApiException("PRODUCT_NOT_FOUND","商品不存在: "+item.getKey(),HttpStatus.NOT_FOUND));if(!"ON_SALE".equals(p.status()))throw new ApiException("PRODUCT_NOT_SALEABLE","商品不可售: "+p.id());if(p.stock()-p.reservedStock()<item.getValue())throw new ApiException("INSUFFICIENT_STOCK","库存不足: "+p.id());}
        Instant expires=Instant.now().plus(ttl,ChronoUnit.SECONDS);long id=insert.executeAndReturnKey(Map.of("idempotency_key",key,"order_id",request.orderId(),"status","RESERVED","expires_at",java.sql.Timestamp.from(expires),"created_at",java.sql.Timestamp.from(Instant.now()),"updated_at",java.sql.Timestamp.from(Instant.now()))).longValue();
        for(var item:quantities.entrySet()){db.sql("insert into inventory_reservation_item(reservation_id,product_id,quantity) values(:reservation,:product,:quantity)").params(Map.of("reservation",id,"product",item.getKey(),"quantity",item.getValue())).update();db.sql("update product set reserved_stock=reserved_stock+:quantity where id=:product").params(Map.of("quantity",item.getValue(),"product",item.getKey())).update();}
        return get(id);
    }
    Reservation get(long id){ReservationRow row=db.sql("select * from inventory_reservation where id=:id").param("id",id).query(ReservationRow.class).optional().orElseThrow(()->new ApiException("RESERVATION_NOT_FOUND","库存预留不存在",HttpStatus.NOT_FOUND));return assemble(row);}
    @Transactional Reservation confirm(long id){ReservationRow row=locked(id);if("CONFIRMED".equals(row.status()))return assemble(row);if(!"RESERVED".equals(row.status()))throw new ApiException("INVALID_RESERVATION_STATE","只有 RESERVED 可确认");if(row.expiresAt().isBefore(Instant.now())){releaseLocked(row,"EXPIRED");throw new ApiException("RESERVATION_EXPIRED","库存预留已过期");}for(var item:items(id)){db.sql("update product set stock=stock-:quantity,reserved_stock=reserved_stock-:quantity where id=:product").params(Map.of("quantity",item.quantity(),"product",item.productId())).update();}setStatus(id,"CONFIRMED");return get(id);}
    @Transactional Reservation release(long id,String target){ReservationRow row=locked(id);if(target.equals(row.status())||"RELEASED".equals(row.status())||"EXPIRED".equals(row.status()))return assemble(row);if("CONFIRMED".equals(row.status()))throw new ApiException("INVALID_RESERVATION_STATE","已确认预留不能释放");releaseLocked(row,target);return get(id);}
    @Scheduled(fixedDelayString="${catalog-shop.expiration-scan-ms:30000}")
    void expire(){List<Long> ids=db.sql("select id from inventory_reservation where status='RESERVED' and expires_at<CURRENT_TIMESTAMP").query(Long.class).list();for(Long id:ids){try{release(id,"EXPIRED");}catch(RuntimeException ignored){/* another node owns the row */}}}
    private void releaseLocked(ReservationRow row,String target){for(var item:items(row.id()))db.sql("update product set reserved_stock=reserved_stock-:quantity where id=:product and reserved_stock>=:quantity").params(Map.of("quantity",item.quantity(),"product",item.productId())).update();setStatus(row.id(),target);String payload="{\"reservationId\":"+row.id()+",\"status\":\""+target+"\"}";String eventType="EXPIRED".equals(target)?"InventoryReservationExpired.v1":"InventoryReservationReleased.v1";db.sql("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload,status) values(:event,'RESERVATION',:aggregate,:eventType,:payload,'PENDING')").params(Map.of("event",UUID.randomUUID().toString(),"aggregate",String.valueOf(row.id()),"eventType",eventType,"payload",payload)).update();}
    private ReservationRow locked(long id){return db.sql("select * from inventory_reservation where id=:id for update").param("id",id).query(ReservationRow.class).optional().orElseThrow(()->new ApiException("RESERVATION_NOT_FOUND","库存预留不存在",HttpStatus.NOT_FOUND));}
    private List<ReservationItem> items(long id){return db.sql("select product_id,quantity from inventory_reservation_item where reservation_id=:id order by product_id").param("id",id).query(ReservationItem.class).list();}
    private Reservation assemble(ReservationRow row){return new Reservation(row.id(),row.idempotencyKey(),row.orderId(),row.status(),row.expiresAt(),items(row.id()));}
    private void setStatus(long id,String status){db.sql("update inventory_reservation set status=:status,updated_at=CURRENT_TIMESTAMP where id=:id").params(Map.of("status",status,"id",id)).update();}
}

record ProductStock(long id,int stock,int reservedStock,String status){}
record ReservationRow(long id,String idempotencyKey,String orderId,String status,Instant expiresAt,Instant createdAt,Instant updatedAt){}
record ReservationItem(long productId,int quantity){}
record Reservation(long id,String idempotencyKey,String orderId,String status,Instant expiresAt,List<ReservationItem> items){}
