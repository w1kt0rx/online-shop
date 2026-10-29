package pl.shop.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
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
import pl.shop.product.dto.request.CreateProductConfigurationRequest;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.exception.ConfigurationNotFoundException;
import pl.shop.product.exception.InvalidConfigurationException;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.service.ProductConfigurationService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductConfigurationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ProductConfigurationService configurationService;

    @Test
    void getProductConfiguration_productExists_returnsConfigurationList() throws Exception {
        //given
        ProductConfigurationResponse gpu = new ProductConfigurationResponse(1L, "Graphics card", 6L, "RTX 4060", true, false);
        ProductConfigurationResponse ram = new ProductConfigurationResponse(3L, "RAM", 7L, "RAM 16 GB DDR5", true, false);
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        when(configurationService.getProductConfiguration(1L)).thenReturn(List.of(gpu, ram));
        //when + then
        mockMvc.perform(get("/products/{productId}/configurations", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].slot").value("Graphics card"))
                .andExpect(jsonPath("$[0].componentName").value("RTX 4060"))
                .andExpect(jsonPath("$[0].defaultComponent").value(true))
                .andExpect(jsonPath("$[1].slot").value("RAM"));
        Mockito.verify(configurationService).getProductConfiguration(idCaptor.capture());
        Assertions.assertEquals(1L, idCaptor.getValue());
    }

    @Test
    void getProductConfiguration_noConfigurations_returnsEmptyList() throws Exception {
        //given
        when(configurationService.getProductConfiguration(5L)).thenReturn(List.of());
        //when + then
        mockMvc.perform(get("/products/{productId}/configurations", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getProductConfiguration_productNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        when(configurationService.getProductConfiguration(999L)).thenThrow(new ProductNotFoundException(999L));
        //when + then
        mockMvc.perform(get("/products/{productId}/configurations", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message", Matchers.containsString("999")));
    }

    @Test
    void createProductConfiguration_dataCorrect_returnsCreatedConfiguration() throws Exception {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(6L, "Graphics card", true, false);
        ProductConfigurationResponse response = new ProductConfigurationResponse(1L, "Graphics card", 6L, "RTX 4060", true, false);
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<CreateProductConfigurationRequest> requestCaptor = ArgumentCaptor.forClass(CreateProductConfigurationRequest.class);
        when(configurationService.createProductConfiguration(anyLong(), any(CreateProductConfigurationRequest.class)))
                .thenReturn(response);
        //when + then
        mockMvc.perform(post("/products/{productId}/configurations", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.slot").value("Graphics card"))
                .andExpect(jsonPath("$.componentId").value(6L))
                .andExpect(jsonPath("$.componentName").value("RTX 4060"))
                .andExpect(jsonPath("$.defaultComponent").value(true))
                .andExpect(jsonPath("$.multiSelect").value(false));
        Mockito.verify(configurationService).createProductConfiguration(productIdCaptor.capture(), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, productIdCaptor.getValue()),
                () -> Assertions.assertEquals(6L, requestCaptor.getValue().componentId()),
                () -> Assertions.assertEquals("Graphics card", requestCaptor.getValue().slot()),
                () -> Assertions.assertTrue(requestCaptor.getValue().defaultComponent()),
                () -> Assertions.assertFalse(requestCaptor.getValue().multiSelect())
        );
    }

    @Test
    void createProductConfiguration_componentIdNull_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(null, "Graphics card", true, false);
        //when + then
        mockMvc.perform(post("/products/{productId}/configurations", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Component ID is required")));
        Mockito.verify(configurationService, Mockito.never()).createProductConfiguration(anyLong(), any());
    }

    @Test
    void createProductConfiguration_slotBlank_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(6L, "", true, false);
        //when + then
        mockMvc.perform(post("/products/{productId}/configurations", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Slot name is required")));
        Mockito.verify(configurationService, Mockito.never()).createProductConfiguration(anyLong(), any());
    }

    @Test
    void createProductConfiguration_slotTooLong_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(6L, "a".repeat(101), true, false);
        //when + then
        mockMvc.perform(post("/products/{productId}/configurations", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Slot name must not exceed 100 characters")));
        Mockito.verify(configurationService, Mockito.never()).createProductConfiguration(anyLong(), any());
    }

    @Test
    void createProductConfiguration_invalidConfiguration_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(4L, "Graphics card", true, false);
        when(configurationService.createProductConfiguration(anyLong(), any(CreateProductConfigurationRequest.class)))
                .thenThrow(new InvalidConfigurationException("Slot already has a default component: Graphics carda"));
        //when + then
        mockMvc.perform(post("/products/{productId}/configurations", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CONFIGURATION"))
                .andExpect(jsonPath("$.message", Matchers.containsString("already has a default component")));
    }

    @Test
    void createProductConfiguration_productNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(6L, "Graphics card", true, false);
        when(configurationService.createProductConfiguration(anyLong(), any(CreateProductConfigurationRequest.class)))
                .thenThrow(new ProductNotFoundException(999L));
        //when + then
        mockMvc.perform(post("/products/{productId}/configurations", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message", Matchers.containsString("999")));
    }

    @Test
    void deleteProductConfiguration_configurationExists_returnsNoContent() throws Exception {
        //given
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> configurationIdCaptor = ArgumentCaptor.forClass(Long.class);
        //when + then
        mockMvc.perform(delete("/products/{productId}/configurations/{configurationId}", 1L, 4L))
                .andExpect(status().isNoContent());
        Mockito.verify(configurationService).deleteProductConfiguration(productIdCaptor.capture(), configurationIdCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, productIdCaptor.getValue()),
                () -> Assertions.assertEquals(4L, configurationIdCaptor.getValue())
        );
    }

    @Test
    void deleteProductConfiguration_configurationNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        Mockito.doThrow(new ConfigurationNotFoundException(999L))
                .when(configurationService).deleteProductConfiguration(1L, 999L);
        //when + then
        mockMvc.perform(delete("/products/{productId}/configurations/{configurationId}", 1L, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONFIGURATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message", Matchers.containsString("999")));
    }
}
