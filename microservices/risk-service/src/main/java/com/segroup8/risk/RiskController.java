package com.segroup8.risk;

import com.segroup8.risk.RiskService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class RiskController {
    private final RiskService service;public RiskController(RiskService service){this.service=service;}
    @PostMapping("/internal/risk-audits") @ResponseStatus(HttpStatus.CREATED)
    Audit create(@Valid @RequestBody CreateRequest r){return service.create(new CreateCommand(r.productId(),r.name(),r.description()));}
    @GetMapping("/api/admin/risk-audits") List<Audit> list(@RequestHeader("X-Admin-Id") long adminId,@RequestParam(required=false) String status){return service.list(status);}
    @PostMapping("/api/admin/risk-audits/{id}/decision") Audit decide(@RequestHeader("X-Admin-Id") long adminId,@PathVariable long id,@Valid @RequestBody DecisionRequest r){return service.decide(id,adminId,new DecisionCommand(r.approved(),r.reason()));}
    public record CreateRequest(@Min(1) long productId,@NotBlank String name,String description){}
    public record DecisionRequest(boolean approved,String reason){}
}
@RestControllerAdvice class RiskErrorHandler{
    @ExceptionHandler(RiskException.class) @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,Object> domain(RiskException e){return Map.of("code",e.code,"message",e.getMessage());}
}
