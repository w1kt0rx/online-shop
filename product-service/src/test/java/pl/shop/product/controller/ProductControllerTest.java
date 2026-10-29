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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.product.dto.request.CreatePriceRequest;
import pl.shop.product.dto.request.CreateProductRequest;
import pl.shop.product.dto.request.PageRequestDto;
import pl.shop.product.dto.request.UpdateProductRequest;
import pl.shop.product.dto.response.PageDto;
import pl.shop.product.dto.response.PriceResponse;
import pl.shop.product.dto.response.ProductResponse;
import pl.shop.product.dto.response.ProductSummaryResponse;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.model.Product;
import pl.shop.product.model.SortDirection;
import pl.shop.product.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_dataCorrect_returnsCreatedProduct() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        PriceResponse priceResponse = new PriceResponse(new BigDecimal("4800.00"), new BigDecimal("0.23"),
                new BigDecimal("1104.00"), new BigDecimal("5904.00"), "PLN");
        ProductResponse response = new ProductResponse(1L, "Gaming PC Pro", "Computer", "MyShop", priceResponse, true, List.of());
        ArgumentCaptor<CreateProductRequest> requestCaptor = ArgumentCaptor.forClass(CreateProductRequest.class);
        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.category").value("Computer"))
                .andExpect(jsonPath("$.price.netPrice").value(4800.00))
                .andExpect(jsonPath("$.price.grossPrice").value(5904.00))
                .andExpect(jsonPath("$.configurations", hasSize(0)));
        Mockito.verify(productService).createProduct(requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("Gaming PC Pro", requestCaptor.getValue().name()),
                () -> Assertions.assertEquals("Computer", requestCaptor.getValue().category()),
                () -> Assertions.assertEquals(new BigDecimal("4800.00"), requestCaptor.getValue().price().netPrice()),
                () -> Assertions.assertEquals("PLN", requestCaptor.getValue().price().currency()),
                () -> Assertions.assertTrue(requestCaptor.getValue().sellableSeparately())
        );
    }

    @Test
    void createProduct_nameBlank_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Product name is required")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_categoryBlank_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", " ", "MyShop",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Category is required")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_vendorBlank_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Vendor is required")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_priceNull_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop", null, true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Price details are required")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_netPriceNegative_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("-1.00"), new BigDecimal("0.23"), "PLN"), true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Net price must be greater than or equal to 0.00")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_netPriceThreeDecimals_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("10.999"), new BigDecimal("0.23"), "PLN"), true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("2 decimal places")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_vatRateAboveOne_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("1.50"), "PLN"), true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("VAT rate cannot exceed 1.00")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_currencyLowercase_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "pln"), true);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("exactly 3 uppercase letters")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_sellableSeparatelyNull_returnsBadRequestWithMessage() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), null);
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Sellable separately flag must be specified")));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void createProduct_malformedJson_returnsBadRequestWithMessage() throws Exception {
        //given
        String malformedJson = "{\"name\": \"Gaming PC Pro\",";
        //when + then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
        Mockito.verify(productService, Mockito.never()).createProduct(any());
    }

    @Test
    void getProduct_productExists_returnsProduct() throws Exception {
        //given
        PriceResponse priceResponse = new PriceResponse(new BigDecimal("4800.00"), new BigDecimal("0.23"),
                new BigDecimal("1104.00"), new BigDecimal("5904.00"), "PLN");
        ProductResponse response = new ProductResponse(1L, "Gaming PC Pro", "Computer", "MyShop", priceResponse, true, List.of());
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        when(productService.getProduct(1L)).thenReturn(response);
        //when + then
        mockMvc.perform(get("/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.vendor").value("MyShop"))
                .andExpect(jsonPath("$.sellableSeparately").value(true));
        Mockito.verify(productService).getProduct(idCaptor.capture());
        Assertions.assertEquals(1L, idCaptor.getValue());
    }

    @Test
    void getProduct_productNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        when(productService.getProduct(999L)).thenThrow(new ProductNotFoundException(999L));
        //when + then
        mockMvc.perform(get("/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message", Matchers.containsString("999")));
    }

    @Test
    void getProduct_idNotNumber_returnsBadRequestWithMessage() throws Exception {
        //when + then
        mockMvc.perform(get("/products/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
        Mockito.verify(productService, Mockito.never()).getProduct(anyLong());
    }

    // ---------- GET /products ----------

    @Test
    void getAllProducts_noParams_defaultPageRequestPassedToService() throws Exception {
        //given
        PriceResponse priceResponse = new PriceResponse(new BigDecimal("4800.00"), new BigDecimal("0.23"),
                new BigDecimal("1104.00"), new BigDecimal("5904.00"), "PLN");
        ProductSummaryResponse summary = new ProductSummaryResponse(1L, "Gaming PC Pro", "Computer", "MyShop", priceResponse, false);
        PageDto<ProductSummaryResponse> page = new PageDto<>(List.of(summary), 0, 20, 1L, 1);
        ArgumentCaptor<String> categoryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PageRequestDto> pageRequestCaptor = ArgumentCaptor.forClass(PageRequestDto.class);
        when(productService.getAllProducts(any(), any(PageRequestDto.class))).thenReturn(page);
        //when + then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        Mockito.verify(productService).getAllProducts(categoryCaptor.capture(), pageRequestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertNull(categoryCaptor.getValue()),
                () -> Assertions.assertEquals(0, pageRequestCaptor.getValue().page()),
                () -> Assertions.assertEquals(20, pageRequestCaptor.getValue().size()),
                () -> Assertions.assertEquals("id", pageRequestCaptor.getValue().sortBy()),
                () -> Assertions.assertEquals(SortDirection.ASC, pageRequestCaptor.getValue().direction())
        );
    }

    @Test
    void getAllProducts_categoryGiven_categoryPassedToService() throws Exception {
        //given
        PriceResponse priceResponse = new PriceResponse(new BigDecimal("4800.00"), new BigDecimal("0.23"),
                new BigDecimal("1104.00"), new BigDecimal("5904.00"), "PLN");
        ProductSummaryResponse summary = new ProductSummaryResponse(1L, "Gaming PC Pro", "Computer", "MyShop", priceResponse, false);
        PageDto<ProductSummaryResponse> page = new PageDto<>(List.of(summary), 0, 20, 1L, 1);
        ArgumentCaptor<String> categoryCaptor = ArgumentCaptor.forClass(String.class);
        when(productService.getAllProducts(any(), any(PageRequestDto.class))).thenReturn(page);
        //when + then
        mockMvc.perform(get("/products").param("category", "Computer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("Computer"));
        Mockito.verify(productService).getAllProducts(categoryCaptor.capture(), any(PageRequestDto.class));
        Assertions.assertEquals("Computer", categoryCaptor.getValue());
    }

    @Test
    void getAllProducts_pagingParamsGiven_paramsPassedToService() throws Exception {
        //given
        PageDto<ProductSummaryResponse> page = new PageDto<>(List.of(), 1, 5, 6L, 2);
        ArgumentCaptor<PageRequestDto> pageRequestCaptor = ArgumentCaptor.forClass(PageRequestDto.class);
        when(productService.getAllProducts(any(), any(PageRequestDto.class))).thenReturn(page);
        //when + then
        mockMvc.perform(get("/products")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(5));
        Mockito.verify(productService).getAllProducts(any(), pageRequestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, pageRequestCaptor.getValue().page()),
                () -> Assertions.assertEquals(5, pageRequestCaptor.getValue().size()),
                () -> Assertions.assertEquals("name", pageRequestCaptor.getValue().sortBy()),
                () -> Assertions.assertEquals(SortDirection.DESC, pageRequestCaptor.getValue().direction())
        );
    }

    @Test
    void getAllProducts_pageSizeZero_returnsBadRequestWithMessage() throws Exception {
        //when + then
        mockMvc.perform(get("/products").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Page size must be at least 1")));
        Mockito.verify(productService, Mockito.never()).getAllProducts(any(), any());
    }

    @Test
    void getAllProducts_pageSizeAboveMax_returnsBadRequestWithMessage() throws Exception {
        //when + then
        mockMvc.perform(get("/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Page size cannot exceed 100")));
        Mockito.verify(productService, Mockito.never()).getAllProducts(any(), any());
    }

    @Test
    void getAllProducts_pageNegative_returnsBadRequestWithMessage() throws Exception {
        //when + then
        mockMvc.perform(get("/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Page number cannot be negative")));
        Mockito.verify(productService, Mockito.never()).getAllProducts(any(), any());
    }

    @Test
    void getAllProducts_directionInvalid_returnsBadRequest() throws Exception {
        //when + then
        mockMvc.perform(get("/products").param("direction", "UP"))
                .andExpect(status().isBadRequest());
        Mockito.verify(productService, Mockito.never()).getAllProducts(any(), any());
    }

    @Test
    void getAllProducts_invalidSortProperty_returnsBadRequestWithMessage() throws Exception {
        //given
        when(productService.getAllProducts(any(), any(PageRequestDto.class)))
                .thenThrow(new PropertyReferenceException("abc", TypeInformation.of(Product.class), List.of()));
        //when + then
        mockMvc.perform(get("/products").param("sortBy", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SORT_PROPERTY"))
                .andExpect(jsonPath("$.message", Matchers.containsString("abc")));
    }

    @Test
    void updateProduct_dataCorrect_returnsUpdatedProduct() throws Exception {
        //given
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), true);
        PriceResponse priceResponse = new PriceResponse(new BigDecimal("2600.00"), new BigDecimal("0.23"),
                new BigDecimal("598.00"), new BigDecimal("3198.00"), "PLN");
        ProductResponse response = new ProductResponse(2L, "Office PC v2", "Computer", "MyShop", priceResponse, true, List.of());
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<UpdateProductRequest> requestCaptor = ArgumentCaptor.forClass(UpdateProductRequest.class);
        when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class))).thenReturn(response);
        //when + then
        mockMvc.perform(put("/products/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Office PC v2"))
                .andExpect(jsonPath("$.price.grossPrice").value(3198.00));
        Mockito.verify(productService).updateProduct(idCaptor.capture(), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(2L, idCaptor.getValue()),
                () -> Assertions.assertEquals("Office PC v2", requestCaptor.getValue().name()),
                () -> Assertions.assertEquals(new BigDecimal("2600.00"), requestCaptor.getValue().price().netPrice())
        );
    }

    @Test
    void updateProduct_productNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), true);
        when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class)))
                .thenThrow(new ProductNotFoundException(999L));
        //when + then
        mockMvc.perform(put("/products/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message", Matchers.containsString("999")));
    }

    @Test
    void updateProduct_nameBlank_returnsBadRequestWithMessage() throws Exception {
        //given
        UpdateProductRequest request = new UpdateProductRequest("", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), true);
        //when + then
        mockMvc.perform(put("/products/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Product name is required")));
        Mockito.verify(productService, Mockito.never()).updateProduct(anyLong(), any());
    }

    @Test
    void updateProduct_priceNull_returnsBadRequestWithMessage() throws Exception {
        //given
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Computer", "MyShop", null, true);
        //when + then
        mockMvc.perform(put("/products/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Price details are required")));
        Mockito.verify(productService, Mockito.never()).updateProduct(anyLong(), any());
    }

    @Test
    void deleteProduct_productExists_returnsNoContent() throws Exception {
        //given
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        //when + then
        mockMvc.perform(delete("/products/{id}", 5L))
                .andExpect(status().isNoContent());
        Mockito.verify(productService).deleteProduct(idCaptor.capture());
        Assertions.assertEquals(5L, idCaptor.getValue());
    }

    @Test
    void deleteProduct_productNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        Mockito.doThrow(new ProductNotFoundException(999L)).when(productService).deleteProduct(999L);
        //when + then
        mockMvc.perform(delete("/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message", Matchers.containsString("999")));
    }

    @Test
    void deleteProduct_productUsedAsComponent_returnsConflictWithMessage() throws Exception {
        //given
        Mockito.doThrow(new DataIntegrityViolationException("fk_product_configurations_component"))
                .when(productService).deleteProduct(6L);
        //when + then
        mockMvc.perform(delete("/products/{id}", 6L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT"));
    }
}