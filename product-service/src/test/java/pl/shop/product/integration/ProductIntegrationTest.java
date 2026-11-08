package pl.shop.product.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.shop.product.dto.request.CreateProductRequest;
import pl.shop.product.dto.request.PriceRequest;
import pl.shop.product.dto.request.UpdateProductRequest;
import pl.shop.product.dto.response.ProductResponse;
import pl.shop.product.kafka.event.ProductCreatedEvent;
import pl.shop.product.kafka.event.ProductDeletedEvent;
import pl.shop.product.model.Price;
import pl.shop.product.model.Product;
import pl.shop.product.repository.ProductRepository;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProductRepository productRepository;
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void cleanup() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_dataCorrect_productSavedAndEventSent() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new PriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when //then
        MvcResult result = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.price.netPrice").value(4800.00))
                .andExpect(jsonPath("$.price.vatAmount").value(1104.00))
                .andExpect(jsonPath("$.price.grossPrice").value(5904.00))
                .andExpect(jsonPath("$.configurations", hasSize(0)))
                .andReturn();
        Long id = objectMapper.readValue(result.getResponse().getContentAsString(), ProductResponse.class).id();
        Product saved = productRepository.findById(id).orElseThrow();
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, productRepository.count()),
                () -> Assertions.assertEquals("Gaming PC Pro", saved.getName()),
                () -> Assertions.assertEquals("Computer", saved.getCategory()),
                () -> Assertions.assertEquals("MyShop", saved.getVendor()),
                () -> Assertions.assertEquals(new BigDecimal("4800.00"), saved.getPrice().getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), saved.getPrice().getVatRate()),
                () -> Assertions.assertEquals("PLN", saved.getPrice().getCurrency()),
                () -> Assertions.assertTrue(saved.isSellableSeparately()),
                () -> Assertions.assertEquals("product-created", topicCaptor.getValue()),
                () -> Assertions.assertEquals(String.valueOf(id), keyCaptor.getValue()),
                () -> Assertions.assertEquals(new ProductCreatedEvent(id, "Gaming PC Pro"), eventCaptor.getValue())
        );
    }

    @Test
    void createProduct_nameBlank_badRequestAndNothingSaved() throws Exception {
        //given
        CreateProductRequest request = new CreateProductRequest("", "Computer", "MyShop",
                new PriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        //when //then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Product name is required")));
        Assertions.assertEquals(0, productRepository.count());
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void getProduct_productExists_returnsProductFromDatabase() throws Exception {
        //given
        Product product = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1219.99"), new BigDecimal("0.23"), "PLN"), true));
        //when //then
        mockMvc.perform(get("/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("RTX 4060"))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.vendor").value("NVIDIA"))
                .andExpect(jsonPath("$.price.netPrice").value(1219.99))
                .andExpect(jsonPath("$.price.vatAmount").value(280.60))
                .andExpect(jsonPath("$.price.grossPrice").value(1500.59))
                .andExpect(jsonPath("$.price.currency").value("PLN"))
                .andExpect(jsonPath("$.sellableSeparately").value(true))
                .andExpect(jsonPath("$.configurations", hasSize(0)));
    }

    @Test
    void getProduct_productNotExists_returnsNotFound() throws Exception {
        //given
        Long id = 999999L;
        //when //then
        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Couldn't find product with id: 999999"));
    }

    @Test
    void getAllProducts_categoryGiven_returnsOnlyMatchingProductsIgnoringCase() throws Exception {
        //given
        productRepository.save(new Product("Office PC", "Computer", "MyShop",
                new Price(new BigDecimal("2500.00"), new BigDecimal("0.23"), "PLN"), true));
        productRepository.save(new Product("Gaming PC", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true));
        productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        //when //then
        mockMvc.perform(get("/products")
                        .param("category", "computer")
                        .param("sortBy", "name")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Gaming PC"))
                .andExpect(jsonPath("$.content[0].category").value("Computer"))
                .andExpect(jsonPath("$.content[0].configurable").value(false))
                .andExpect(jsonPath("$.content[1].name").value("Office PC"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllProducts_pagingAndSortGiven_returnsRequestedPage() throws Exception {
        //given
        productRepository.save(new Product("Office PC", "Computer", "MyShop",
                new Price(new BigDecimal("2500.00"), new BigDecimal("0.23"), "PLN"), true));
        productRepository.save(new Product("Gaming PC", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true));
        productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        //when //then
        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "name")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("RTX 4060"))
                .andExpect(jsonPath("$.content[1].name").value("Office PC"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/products")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "name")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Gaming PC"))
                .andExpect(jsonPath("$.pageNumber").value(1));
    }

    @Test
    void getAllProducts_invalidSortProperty_returnsBadRequest() throws Exception {
        //given
        productRepository.save(new Product("Office PC", "Computer", "MyShop",
                new Price(new BigDecimal("2500.00"), new BigDecimal("0.23"), "PLN"), true));
        //when //then
        mockMvc.perform(get("/products").param("sortBy", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SORT_PROPERTY"))
                .andExpect(jsonPath("$.message", Matchers.containsString("abc")));
    }

    @Test
    void updateProduct_dataCorrect_changesPersisted() throws Exception {
        //given
        Product product = productRepository.save(new Product("Office PC", "Computer", "MyShop",
                new Price(new BigDecimal("2500.00"), new BigDecimal("0.23"), "PLN"), true));
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Workstation", "OtherShop",
                new PriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.08"), "EUR"), false);
        //when //then
        mockMvc.perform(put("/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Office PC v2"))
                .andExpect(jsonPath("$.price.vatAmount").value(208.00))
                .andExpect(jsonPath("$.price.grossPrice").value(2808.00))
                .andExpect(jsonPath("$.price.currency").value("EUR"))
                .andExpect(jsonPath("$.sellableSeparately").value(false));
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertAll(
                () -> Assertions.assertEquals("Office PC v2", updated.getName()),
                () -> Assertions.assertEquals("Workstation", updated.getCategory()),
                () -> Assertions.assertEquals("OtherShop", updated.getVendor()),
                () -> Assertions.assertEquals(new BigDecimal("2600.00"), updated.getPrice().getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.08"), updated.getPrice().getVatRate()),
                () -> Assertions.assertEquals("EUR", updated.getPrice().getCurrency()),
                () -> Assertions.assertFalse(updated.isSellableSeparately())
        );
    }

    @Test
    void updateProduct_productNotExists_returnsNotFoundAndNothingSaved() throws Exception {
        //given
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Computer", "MyShop",
                new PriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), true);
        //when //then
        mockMvc.perform(put("/products/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Couldn't find product with id: 999999"));
        Assertions.assertEquals(0, productRepository.count());
    }

    @Test
    void deleteProduct_productExists_productRemovedAndEventSent() throws Exception {
        //given
        Product product = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when //then
        mockMvc.perform(delete("/products/{id}", product.getId()))
                .andExpect(status().isNoContent());
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertFalse(productRepository.existsById(product.getId())),
                () -> Assertions.assertEquals("product-deleted", topicCaptor.getValue()),
                () -> Assertions.assertEquals(String.valueOf(product.getId()), keyCaptor.getValue()),
                () -> Assertions.assertEquals(new ProductDeletedEvent(product.getId(), "RTX 4060"), eventCaptor.getValue())
        );
    }

    @Test
    void deleteProduct_productNotExists_returnsNotFoundAndNoEventSent() throws Exception {
        //given
        Product product = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        //when //then
        mockMvc.perform(delete("/products/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
        Assertions.assertAll(
                () -> Assertions.assertTrue(productRepository.existsById(product.getId())),
                () -> Assertions.assertEquals(1, productRepository.count())
        );
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }
}
