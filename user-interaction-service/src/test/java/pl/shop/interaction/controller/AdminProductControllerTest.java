package pl.shop.interaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.interaction.dto.request.CreateProductConfigurationRequest;
import pl.shop.interaction.dto.request.CreateProductRequest;
import pl.shop.interaction.dto.request.PriceRequest;
import pl.shop.interaction.dto.request.UpdateProductRequest;
import pl.shop.interaction.dto.response.ProductConfigurationResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.service.AdminCatalogService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AdminCatalogService adminCatalogService;

    @Test
    void createProduct_dataCorrect_returnsCreatedProduct() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("RTX 4070", "Electronics", "NVIDIA",
                new PriceRequest(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ArgumentCaptor<CreateProductRequest> requestCaptor = ArgumentCaptor.forClass(CreateProductRequest.class);
        when(adminCatalogService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(new ProductResponse(2L, "RTX 4070", "Electronics", "NVIDIA", null, true, List.of()));
        //when //then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L));
        Mockito.verify(adminCatalogService).createProduct(requestCaptor.capture());
        Assertions.assertEquals("NVIDIA", requestCaptor.getValue().vendor());
    }

    @Test
    void createProduct_validationFailedInProductService_returnsBadRequestWithRemoteError() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.POST, "http://localhost:8081/products", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":400,\"errorCode\":\"ARGUMENT_NOT_VALID\",\"message\":\"Validation failed: name: Product name is required\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(adminCatalogService.createProduct(any(CreateProductRequest.class))).thenThrow(new FeignException.BadRequest("Bad Request", request, body, null));
        //when //then
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Electronics\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed: name: Product name is required"));
    }

    @Test
    void updateProduct_dataCorrect_requestPassed() throws Exception {
        //given
        UpdateProductRequest request = new UpdateProductRequest("RTX 4070 Super", "Electronics", "NVIDIA",
                new PriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), true);
        when(adminCatalogService.updateProduct(anyLong(), any(UpdateProductRequest.class)))
                .thenReturn(new ProductResponse(2L, "RTX 4070 Super", "Electronics", "NVIDIA", null, true, List.of()));
        //when //then
        mockMvc.perform(put("/api/admin/products/{productId}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("RTX 4070 Super"));
    }

    @Test
    void deleteProduct_productUsedAsComponent_returnsConflictWithRemoteError() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.DELETE, "http://localhost:8081/products/1", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":409,\"errorCode\":\"DATA_CONFLICT\",\"message\":\"Operation violates data constraints\"}".getBytes(StandardCharsets.UTF_8);
        Mockito.doThrow(new FeignException.Conflict("Conflict", request, body, null)).when(adminCatalogService).deleteProduct(1L);
        //when //then
        mockMvc.perform(delete("/api/admin/products/{productId}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT"));
    }

    @Test
    void deleteProduct_productExists_returnsNoContent() throws Exception {
        //given
        Long productId = 6L;
        //when //then
        mockMvc.perform(delete("/api/admin/products/{productId}", productId))
                .andExpect(status().isNoContent());
        Mockito.verify(adminCatalogService).deleteProduct(6L);
    }

    @Test
    void createConfiguration_dataCorrect_returnsCreatedConfiguration() throws Exception {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(1L, "Graphics card", true, false);
        when(adminCatalogService.createConfiguration(anyLong(), any(CreateProductConfigurationRequest.class)))
                .thenReturn(new ProductConfigurationResponse(1L, "Graphics card", 1L, "RTX 4060", true, false));
        //when //then
        mockMvc.perform(post("/api/admin/products/{productId}/configurations", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.componentName").value("RTX 4060"));
        Mockito.verify(adminCatalogService).createConfiguration(Mockito.eq(5L), any(CreateProductConfigurationRequest.class));
    }

    @Test
    void deleteConfiguration_idsGiven_returnsNoContent() throws Exception {
        //given
        Long configurationId = 3L;
        //when //then
        mockMvc.perform(delete("/api/admin/products/{productId}/configurations/{configurationId}", 5L, configurationId))
                .andExpect(status().isNoContent());
        Mockito.verify(adminCatalogService).deleteConfiguration(5L, 3L);
    }
}
