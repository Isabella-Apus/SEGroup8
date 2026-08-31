package com.segroup8.behavior;

import com.segroup8.behavior.BehaviorService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/behavior")
public class BehaviorController{
    private final BehaviorService service;public BehaviorController(BehaviorService service){this.service=service;}
    @PostMapping("/browse-history") @ResponseStatus(HttpStatus.CREATED) void browse(@RequestHeader("X-User-Id") long userId,@Valid @RequestBody BrowseRequest r){service.recordBrowse(userId,new BrowseCommand(r.productId(),r.productType()));}
    @GetMapping("/browse-history") List<Browse> browseList(@RequestHeader("X-User-Id") long userId){return service.browse(userId);}
    @DeleteMapping("/browse-history/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@RequestHeader("X-User-Id") long userId,@PathVariable long id){service.deleteBrowse(userId,id);}
    @DeleteMapping("/browse-history") @ResponseStatus(HttpStatus.NO_CONTENT) void clear(@RequestHeader("X-User-Id") long userId){service.clearBrowse(userId);}
    @PostMapping("/search-history") @ResponseStatus(HttpStatus.CREATED) void search(@RequestHeader("X-User-Id") long userId,@Valid @RequestBody SearchRequest r){service.recordSearch(userId,r.keyword());}
    @GetMapping("/search-history") List<Search> searches(@RequestHeader("X-User-Id") long userId){return service.searches(userId);}
    @GetMapping("/hot-keywords") List<HotKeyword> hot(){return service.hot();}
    public record BrowseRequest(@Min(1) long productId,@NotBlank String productType){}
    public record SearchRequest(@NotBlank String keyword){}
}
@RestControllerAdvice class BehaviorErrorHandler{
    @ExceptionHandler(BehaviorException.class) @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,Object> domain(BehaviorException e){return Map.of("code",e.code,"message",e.getMessage());}
}
