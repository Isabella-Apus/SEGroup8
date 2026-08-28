package com.segroup8.platform.controller;

import com.segroup8.platform.common.GlobalExceptionHandler;
import com.segroup8.platform.dto.SecondhandProductPageQueryRequest;
import com.segroup8.platform.dto.SecondhandProductSaveRequest;
import com.segroup8.platform.service.SearchBehaviorService;
import com.segroup8.platform.service.SecondhandProductService;
import com.segroup8.platform.vo.SecondhandProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("DOMAIN_D")
@Tag("UC16")
@ExtendWith(MockitoExtension.class)
class SecondhandProductControllerUc16WebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private SecondhandProductService secondhandProductService;

    @Mock
    private SearchBehaviorService searchBehaviorService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        SecondhandProductController controller =
                new SecondhandProductController(secondhandProductService, searchBehaviorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void sellerCreate_shouldValidateRequestAndReturnProduct() throws Exception {
        SecondhandProductVO vo = new SecondhandProductVO();
        vo.setId(16L);
        vo.setName("二手教材");
        vo.setSalePrice(new BigDecimal("68.00"));

        when(secondhandProductService.createSellerProduct(any(SecondhandProductSaveRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/secondhand/seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "二手教材",
                                  "images": ["/uploads/uc16-book.png"],
                                  "originPrice": 120.00,
                                  "salePrice": 68.00,
                                  "categoryId": 1,
                                  "subCategoryId": 101,
                                  "conditionLevel": "LIKE_NEW",
                                  "isNegotiable": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(16))
                .andExpect(jsonPath("$.data.name").value("二手教材"));

        verify(secondhandProductService).createSellerProduct(any(SecondhandProductSaveRequest.class));
    }

    @Test
    void publicList_shouldRecordKeywordAndDelegateQuery() throws Exception {
        mockMvc.perform(get("/api/secondhand/list")
                        .param("keyword", "教材")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(searchBehaviorService).recordKeyword("教材");
        verify(secondhandProductService).pagePublicProducts(any(SecondhandProductPageQueryRequest.class));
    }
}
