package com.segroup8.shop;

import com.segroup8.shop.ShopService.DecorationCommand;
import com.segroup8.shop.ShopService.SettingsCommand;
import com.segroup8.shop.ShopService.Shop;
import com.segroup8.shop.ShopService.ShopView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/shops")
public class ShopController {
    private final ShopService service; public ShopController(ShopService service){this.service=service;}
    @GetMapping("/{id}") ShopView publicView(@PathVariable long id){return service.publicView(id);}
    @GetMapping("/seller/current") Shop mine(@RequestHeader("X-Seller-Id") long sellerId){return service.mine(sellerId);}
    @PutMapping("/seller/current/settings") Shop settings(@RequestHeader("X-Seller-Id") long sellerId,@Valid @RequestBody SettingsRequest r){return service.settings(sellerId,r.command());}
    @PutMapping("/seller/current/decoration") Shop decoration(@RequestHeader("X-Seller-Id") long sellerId,@Valid @RequestBody DecorationRequest r){return service.decorate(sellerId,r.command());}
    public record SettingsRequest(@NotBlank @Size(max=80) String name,@Size(max=500) String announcement,boolean open){SettingsCommand command(){return new SettingsCommand(name,announcement,open);}}
    public record DecorationRequest(@NotBlank String template,@NotBlank String contentJson){DecorationCommand command(){return new DecorationCommand(template,contentJson);}}
}
@RestControllerAdvice class ShopErrorHandler {
    @ExceptionHandler(ShopException.class) @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,Object> domain(ShopException e){return Map.of("code",e.code,"message",e.getMessage());}
}
