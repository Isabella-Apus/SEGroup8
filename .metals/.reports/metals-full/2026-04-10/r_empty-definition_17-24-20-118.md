error id: file:///C:/Users/29382/Desktop/VS%20code/software/SEGroup8/backend/src/main/java/com/segroup8/platform/controller/UserController.java:com/segroup8/platform/vo/BrowseHistoryVO#
file:///C:/Users/29382/Desktop/VS%20code/software/SEGroup8/backend/src/main/java/com/segroup8/platform/controller/UserController.java
empty definition using pc, found symbol in pc: com/segroup8/platform/vo/BrowseHistoryVO#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 459
uri: file:///C:/Users/29382/Desktop/VS%20code/software/SEGroup8/backend/src/main/java/com/segroup8/platform/controller/UserController.java
text:
```scala
package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.AddressSaveRequest;
import com.segroup8.platform.dto.MerchantApplicationSubmitRequest;
import com.segroup8.platform.dto.UserProfileUpdateRequest;
import com.segroup8.platform.service.BrowseHistoryService;
import com.segroup8.platform.service.UserService;
import com.segroup8.platform.vo.AddressVO;
import com.segroup8.platform.vo.@@BrowseHistoryVO;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final BrowseHistoryService browseHistoryService;

    public UserController(UserService userService, BrowseHistoryService browseHistoryService) {
        this.userService = userService;
        this.browseHistoryService = browseHistoryService;
    }

    @Operation(summary = "获取当前登录用户资料")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":{\"id\":3,\"username\":\"user\",\"nickname\":\"DemoUser\",\"role\":\"USER\"}}")))
    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getCurrentUserProfile());
    }

    @Operation(summary = "兼容旧接口-获取当前登录用户")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUserProfile());
    }

    @Operation(summary = "更新个人资料")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileUpdateRequest.class), examples = @ExampleObject(value = "{\n  \"nickname\": \"新昵称\",\n  \"avatar\": \"/uploads/avatar.jpg\",\n  \"phone\": \"13800138000\",\n  \"email\": \"user@demo.com\"\n}")))
    @ApiResponse(responseCode = "200", description = "更新成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}")))
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        userService.updateCurrentUserProfile(request);
        return Result.success();
    }

    @Operation(summary = "地址列表")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":[{\"id\":1,\"receiverName\":\"张三\",\"receiverPhone\":\"13800138000\",\"province\":\"北京市\",\"city\":\"北京市\",\"detailAddress\":\"中关村\",\"isDefault\":1}]}")))
    @GetMapping("/addresses")
    public Result<List<AddressVO>> listAddresses() {
        return Result.success(userService.listMyAddresses());
    }

    @Operation(summary = "新增地址")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddressSaveRequest.class), examples = @ExampleObject(value = "{\n  \"receiverName\": \"李四\",\n  \"receiverPhone\": \"13800138001\",\n  \"province\": \"北京市\",\n  \"city\": \"北京市\",\n  \"detailAddress\": \"海淀区XX路\",\n  \"isDefault\": 1\n}")))
    @ApiResponse(responseCode = "200", description = "新增成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}")))
    @PostMapping("/addresses")
    public Result<Void> createAddress(@Valid @RequestBody AddressSaveRequest request) {
        userService.createAddress(request);
        return Result.success();
    }

    @Operation(summary = "编辑地址")
    @PutMapping("/addresses/{addressId}")
    public Result<Void> updateAddress(@PathVariable Long addressId, @Valid @RequestBody AddressSaveRequest request) {
        userService.updateAddress(addressId, request);
        return Result.success();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/addresses/{addressId}")
    public Result<Void> deleteAddress(@PathVariable Long addressId) {
        userService.deleteAddress(addressId);
        return Result.success();
    }

    @Operation(summary = "提交商家入驻申请")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = MerchantApplicationSubmitRequest.class), examples = @ExampleObject(value = "{\n  \"storeName\": \"好物小店\",\n  \"categoryId\": 2,\n  \"idCardNo\": \"110101199001011234\",\n  \"bankCardNo\": \"6222021234567890\",\n  \"licenseImg\": \"/uploads/license.jpg\",\n  \"warehouseProvince\": \"北京市\",\n  \"warehouseCity\": \"北京市\",\n  \"warehouseDetail\": \"海淀区中关村软件园\",\n  \"warehouseAddr\": \"北京市 北京市 海淀区中关村软件园\",\n  \"contactName\": \"王五\",\n  \"contactPhone\": \"13800138002\"\n}")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "提交成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":null}"))),
            @ApiResponse(responseCode = "400", description = "重复提交", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":400,\"message\":\"已提交申请，请等待审核\",\"data\":null}")))
    })
    @PostMapping("/merchant-application")
    public Result<Void> submitMerchantApplication(@Valid @RequestBody MerchantApplicationSubmitRequest request) {
        userService.submitMerchantApplication(request);
        return Result.success();
    }

    @Operation(summary = "查看我的入驻申请")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":{\"id\":8,\"storeName\":\"好物小店\",\"status\":0,\"rejectReason\":null}}")))
    @GetMapping({ "/merchant-application/me", "/merchant-application/me." })
    public Result<MerchantApplicationVO> getMyMerchantApplication() {
        return Result.success(userService.getMyMerchantApplication());
    }

    @Operation(summary = "保存浏览记录")
    @PostMapping("/browse-history")
    public Result<Void> saveBrowseHistory(@RequestBody Map<String, Long> request) {
        Long productId = request.get("productId");
        if (productId != null) {
            browseHistoryService.saveBrowseHistory(productId);
        }
        return Result.success();
    }

    @Operation(summary = "获取浏览记录")
    @GetMapping("/browse-history")
    public Result<List<BrowseHistoryVO>> getBrowseHistory() {
        return Result.success(browseHistoryService.getBrowseHistory());
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: com/segroup8/platform/vo/BrowseHistoryVO#