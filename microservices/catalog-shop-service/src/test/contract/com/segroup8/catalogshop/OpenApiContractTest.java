package com.segroup8.catalogshop;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest @AutoConfigureMockMvc @Tag("DOMAIN_B")
class OpenApiContractTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;
    @Test @SuppressWarnings("unchecked") void controllerStaticContractAndRuntimeAreIdentical()throws Exception{
        String runtimeText=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode runtime=json.readTree(runtimeText).path("paths");Map<String,Map<String,String>> actual=new LinkedHashMap<>();
        runtime.fields().forEachRemaining(path->{Map<String,String> methods=new LinkedHashMap<>();path.getValue().fields().forEachRemaining(method->{if(Set.of("get","post","put","delete","patch").contains(method.getKey()))methods.put(method.getKey(),method.getValue().path("operationId").asText());});actual.put(path.getKey(),methods);});
        Path contract=Path.of("..","..","02_docs","microservices","catalog-shop-service","openapi.yaml");Map<String,Object> root=new Yaml().load(Files.readString(contract));Map<String,Object> paths=(Map<String,Object>)root.get("paths");Map<String,Map<String,String>> expected=new LinkedHashMap<>();
        paths.forEach((path,value)->{Map<String,String> methods=new LinkedHashMap<>();((Map<String,Object>)value).forEach((method,operation)->{if(Set.of("get","post","put","delete","patch").contains(method))methods.put(method,String.valueOf(((Map<String,Object>)operation).get("operationId")));});expected.put(path,methods);});
        assertEquals(expected,actual,"静态 OpenAPI、Controller 和运行时 /v3/api-docs 必须完全一致");
        Set<String> operationIds=new LinkedHashSet<>();actual.values().forEach(methods->methods.values().forEach(id->assertTrue(operationIds.add(id),"重复 operationId: "+id)));
        assertFalse(runtimeText.contains("X-Seller-Id"));assertFalse(runtimeText.contains("X-Admin-Id"));assertFalse(runtimeText.contains("X-User-Id"));
    }
}
