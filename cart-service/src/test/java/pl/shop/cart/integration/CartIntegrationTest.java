package pl.shop.cart.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.wiremock.spring.EnableWireMock;
import pl.shop.cart.dto.request.AddCartItemRequest;
import pl.shop.cart.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.cart.dto.response.CartResponse;
import pl.shop.cart.model.Cart;
import pl.shop.cart.model.CartItem;
import pl.shop.cart.model.CartItemComponent;
import pl.shop.cart.model.CartStatus;
import pl.shop.cart.model.Price;
import pl.shop.cart.repository.CartRepository;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
public class CartIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        cartRepository.deleteAll();
    }

    @Test
    void createCart_noBody_activeEmptyCartCreatedAndSaved() throws Exception {
        //given
        long cartsBefore = cartRepository.count();
        //when
        String content = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total.netPrice").value(0))
                .andExpect(jsonPath("$.total.vatAmount").value(0))
                .andExpect(jsonPath("$.total.grossPrice").value(0))
                .andExpect(jsonPath("$.total.currency").value("PLN"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        //then
        CartResponse response = objectMapper.readValue(content, CartResponse.class);
        Cart saved = cartRepository.findById(response.id()).orElseThrow();
        Assertions.assertAll(
                () -> Assertions.assertEquals(cartsBefore + 1, cartRepository.count()),
                () -> Assertions.assertEquals(CartStatus.ACTIVE, saved.getStatus()),
                () -> Assertions.assertNotNull(saved.getCreatedAt())
        );
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void addItem_stockAvailable_itemSavedWithPriceSnapshotAndTotalsCalculated() throws Exception {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 3);
        stubFor(WireMock.post(urlEqualTo("/products/1/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": [11]}
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "productId": 1,
                                  "productName": "Gaming PC Pro",
                                  "selectedComponents": [
                                    {"configurationId": 11, "componentProductId": 4, "slot": "Graphics card", "componentName": "RTX 4070"}
                                  ],
                                  "unitPrice": {"netPrice": 1999.99, "vatRate": 0.23, "vatAmount": 460.00, "grossPrice": 2459.99, "currency": "PLN"},
                                  "stockProductIds": [1, 4]
                                }
                                """)));
        stubFor(WireMock.get(urlEqualTo("/inventory?productIds=1&productIds=4"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 1, "quantity": 3, "available": true},
                                  {"productId": 4, "quantity": 5, "available": true}
                                ]
                                """)));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].productId").value(1L))
                .andExpect(jsonPath("$.items[0].productName").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].unitPrice.netPrice").value(1999.99))
                .andExpect(jsonPath("$.items[0].unitPrice.vatRate").value(0.23))
                .andExpect(jsonPath("$.items[0].unitPrice.vatAmount").value(460.00))
                .andExpect(jsonPath("$.items[0].unitPrice.grossPrice").value(2459.99))
                .andExpect(jsonPath("$.items[0].unitPrice.currency").value("PLN"))
                .andExpect(jsonPath("$.items[0].totalPrice.netPrice").value(5999.97))
                .andExpect(jsonPath("$.items[0].totalPrice.vatAmount").value(1379.99))
                .andExpect(jsonPath("$.items[0].totalPrice.grossPrice").value(7379.96))
                .andExpect(jsonPath("$.items[0].components", hasSize(1)))
                .andExpect(jsonPath("$.items[0].components[0].configurationId").value(11L))
                .andExpect(jsonPath("$.items[0].components[0].componentProductId").value(4L))
                .andExpect(jsonPath("$.items[0].components[0].slot").value("Graphics card"))
                .andExpect(jsonPath("$.items[0].components[0].componentName").value("RTX 4070"))
                .andExpect(jsonPath("$.total.netPrice").value(5999.97))
                .andExpect(jsonPath("$.total.vatAmount").value(1379.99))
                .andExpect(jsonPath("$.total.grossPrice").value(7379.96))
                .andExpect(jsonPath("$.total.currency").value("PLN"));
        verify(1, postRequestedFor(urlEqualTo("/products/1/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": [11]}
                        """)));
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1&productIds=4")));
        List<LoggedRequest> stockRequests = WireMock.findAll(getRequestedFor(urlPathEqualTo("/inventory")));
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, stockRequests.size()),
                () -> Assertions.assertEquals(List.of("1", "4"), stockRequests.getFirst().queryParameter("productIds").values())
        );
        transactionTemplate.executeWithoutResult(transaction -> {
            Cart cart = cartRepository.findById(cartId).orElseThrow();
            CartItem item = cart.getItems().getFirst();
            CartItemComponent component = item.getComponents().getFirst();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1, cart.getItems().size()),
                    () -> Assertions.assertEquals(1L, item.getProductId()),
                    () -> Assertions.assertEquals("Gaming PC Pro", item.getProductName()),
                    () -> Assertions.assertEquals(3, item.getQuantity()),
                    () -> Assertions.assertEquals(0, new BigDecimal("1999.99").compareTo(item.getUnitPrice().getNetPrice())),
                    () -> Assertions.assertEquals(0, new BigDecimal("0.23").compareTo(item.getUnitPrice().getVatRate())),
                    () -> Assertions.assertEquals("PLN", item.getUnitPrice().getCurrency()),
                    () -> Assertions.assertEquals(1, item.getComponents().size()),
                    () -> Assertions.assertEquals(11L, component.getConfigurationId()),
                    () -> Assertions.assertEquals(4L, component.getComponentProductId()),
                    () -> Assertions.assertEquals("Graphics card", component.getSlot()),
                    () -> Assertions.assertEquals("RTX 4070", component.getComponentName()),
                    () -> Assertions.assertEquals(0, new BigDecimal("5999.97").compareTo(cart.getTotalNet())),
                    () -> Assertions.assertEquals(0, new BigDecimal("1379.99").compareTo(cart.getTotalVat())),
                    () -> Assertions.assertEquals(0, new BigDecimal("7379.96").compareTo(cart.getTotalGross()))
            );
        });
    }

    @Test
    void addItem_sameProductAndConfiguration_quantityMergedIntoExistingItem() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 2,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        Cart saved = cartRepository.save(cart);
        Long cartId = saved.getId();
        Long itemId = saved.getItems().getFirst().getId();
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 1);
        stubFor(WireMock.post(urlEqualTo("/products/1/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": [11]}
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "productId": 1,
                                  "productName": "Gaming PC Pro",
                                  "selectedComponents": [
                                    {"configurationId": 11, "componentProductId": 4, "slot": "Graphics card", "componentName": "RTX 4070"}
                                  ],
                                  "unitPrice": {"netPrice": 5855.00, "vatRate": 0.23, "vatAmount": 1346.65, "grossPrice": 7201.65, "currency": "PLN"},
                                  "stockProductIds": [1, 4]
                                }
                                """)));
        stubFor(WireMock.get(urlEqualTo("/inventory?productIds=1&productIds=4"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 1, "quantity": 3, "available": true},
                                  {"productId": 4, "quantity": 3, "available": true}
                                ]
                                """)));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(itemId))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].totalPrice.netPrice").value(17565.00))
                .andExpect(jsonPath("$.items[0].totalPrice.vatAmount").value(4039.95))
                .andExpect(jsonPath("$.items[0].totalPrice.grossPrice").value(21604.95))
                .andExpect(jsonPath("$.total.grossPrice").value(21604.95));
        verify(1, postRequestedFor(urlEqualTo("/products/1/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": [11]}
                        """)));
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1&productIds=4")));
        transactionTemplate.executeWithoutResult(transaction -> {
            Cart reloaded = cartRepository.findById(cartId).orElseThrow();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1, reloaded.getItems().size()),
                    () -> Assertions.assertEquals(itemId, reloaded.getItems().getFirst().getId()),
                    () -> Assertions.assertEquals(3, reloaded.getItems().getFirst().getQuantity()),
                    () -> Assertions.assertEquals(1, reloaded.getItems().getFirst().getComponents().size()),
                    () -> Assertions.assertEquals(0, new BigDecimal("21604.95").compareTo(reloaded.getTotalGross()))
            );
        });
    }

    @Test
    void addItem_cumulativeQuantityExceedsStock_conflictReturnedAndNothingPersisted() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 2,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        Long cartId = cartRepository.save(cart).getId();
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 2);
        stubFor(WireMock.post(urlEqualTo("/products/1/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": [11]}
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "productId": 1,
                                  "productName": "Gaming PC Pro",
                                  "selectedComponents": [
                                    {"configurationId": 11, "componentProductId": 4, "slot": "Graphics card", "componentName": "RTX 4070"}
                                  ],
                                  "unitPrice": {"netPrice": 5855.00, "vatRate": 0.23, "vatAmount": 1346.65, "grossPrice": 7201.65, "currency": "PLN"},
                                  "stockProductIds": [1, 4]
                                }
                                """)));
        stubFor(WireMock.get(urlEqualTo("/inventory?productIds=1&productIds=4"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 1, "quantity": 10, "available": true},
                                  {"productId": 4, "quantity": 3, "available": true}
                                ]
                                """)));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 4. Required: 4, available: 3"));
        verify(1, postRequestedFor(urlEqualTo("/products/1/configurations/quote")));
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1&productIds=4")));
        transactionTemplate.executeWithoutResult(transaction -> {
            Cart reloaded = cartRepository.findById(cartId).orElseThrow();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1, reloaded.getItems().size()),
                    () -> Assertions.assertEquals(2, reloaded.getItems().getFirst().getQuantity()),
                    () -> Assertions.assertEquals(0, new BigDecimal("11710.00").compareTo(reloaded.getTotalNet()))
            );
        });
    }

    @Test
    void addItem_productMissingInInventoryResponse_conflictReturnedWithZeroAvailable() throws Exception {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        stubFor(WireMock.post(urlEqualTo("/products/5/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": []}
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "productId": 5,
                                  "productName": "Headphones ABC",
                                  "selectedComponents": [],
                                  "unitPrice": {"netPrice": 240.00, "vatRate": 0.23, "vatAmount": 55.20, "grossPrice": 295.20, "currency": "PLN"},
                                  "stockProductIds": [5]
                                }
                                """)));
        stubFor(WireMock.get(urlEqualTo("/inventory?productIds=5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 5. Required: 1, available: 0"));
        verify(1, postRequestedFor(urlEqualTo("/products/5/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": []}
                        """)));
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=5")));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertTrue(cartRepository.findById(cartId).orElseThrow().getItems().isEmpty()));
    }

    @Test
    void addItem_productServiceReturns404_notFoundWithRemoteErrorCodeReturnedAndInventoryNotCalled() throws Exception {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        AddCartItemRequest request = new AddCartItemRequest(999L, List.of(), 1);
        stubFor(WireMock.post(urlEqualTo("/products/999/configurations/quote"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-01T12:00:00", "status": 404, "errorCode": "PRODUCT_NOT_FOUND", "message": "Couldn't find product with id: 999"}
                                """)));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Couldn't find product with id: 999"));
        verify(1, postRequestedFor(urlEqualTo("/products/999/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": []}
                        """)));
        verify(0, getRequestedFor(urlPathEqualTo("/inventory")));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertTrue(cartRepository.findById(cartId).orElseThrow().getItems().isEmpty()));
    }

    @Test
    void addItem_productServiceReturns503_requestRetriedFiveTimesAndServiceUnavailableReturned() throws Exception {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 1);
        stubFor(WireMock.post(urlEqualTo("/products/1/configurations/quote"))
                .willReturn(aResponse().withStatus(503)));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Remote service is unavailable, try again later"));
        verify(5, postRequestedFor(urlEqualTo("/products/1/configurations/quote"))
                .withRequestBody(equalToJson("""
                        {"configurationIds": [11]}
                        """)));
        verify(0, getRequestedFor(urlPathEqualTo("/inventory")));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertTrue(cartRepository.findById(cartId).orElseThrow().getItems().isEmpty()));
    }

    @Test
    void addItem_inventoryServiceReturns500_remoteServiceErrorReturnedWithoutRetry() throws Exception {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        stubFor(WireMock.post(urlEqualTo("/products/5/configurations/quote"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "productId": 5,
                                  "productName": "Headphones ABC",
                                  "selectedComponents": [],
                                  "unitPrice": {"netPrice": 240.00, "vatRate": 0.23, "vatAmount": 55.20, "grossPrice": 295.20, "currency": "PLN"},
                                  "stockProductIds": [5]
                                }
                                """)));
        stubFor(WireMock.get(urlEqualTo("/inventory?productIds=5"))
                .willReturn(aResponse().withStatus(500)));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("REMOTE_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("Remote service failed, try again later"));
        verify(1, postRequestedFor(urlEqualTo("/products/5/configurations/quote")));
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=5")));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertTrue(cartRepository.findById(cartId).orElseThrow().getItems().isEmpty()));
    }

    @Test
    void addItem_quantityZero_badRequestReturnedWithoutRemoteCalls() throws Exception {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 0);
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("quantity: Quantity must be at least 1")));
        verify(0, anyRequestedFor(anyUrl()));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertTrue(cartRepository.findById(cartId).orElseThrow().getItems().isEmpty()));
    }

    @Test
    void addItem_cartNotExists_notFoundReturnedWithoutRemoteCalls() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 1);
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CART_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cart with id = 999999 doesn't exist"));
        verify(0, anyRequestedFor(anyUrl()));
        Assertions.assertFalse(cartRepository.existsById(999999L));
    }

    @Test
    void changeItemQuantity_quantityIncreasedAndStockAvailable_itemUpdatedAndTotalsRecalculated() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Cart saved = cartRepository.save(cart);
        Long cartId = saved.getId();
        Long itemId = saved.getItems().getFirst().getId();
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(3);
        stubFor(WireMock.get(urlEqualTo("/inventory?productIds=5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 5, "quantity": 3, "available": true}
                                ]
                                """)));
        //when //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(itemId))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].totalPrice.netPrice").value(720.00))
                .andExpect(jsonPath("$.items[0].totalPrice.vatAmount").value(165.60))
                .andExpect(jsonPath("$.items[0].totalPrice.grossPrice").value(885.60))
                .andExpect(jsonPath("$.total.netPrice").value(720.00))
                .andExpect(jsonPath("$.total.vatAmount").value(165.60))
                .andExpect(jsonPath("$.total.grossPrice").value(885.60));
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=5")));
        verify(0, postRequestedFor(urlPathEqualTo("/products/5/configurations/quote")));
        transactionTemplate.executeWithoutResult(transaction -> {
            Cart reloaded = cartRepository.findById(cartId).orElseThrow();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(3, reloaded.findItem(itemId).getQuantity()),
                    () -> Assertions.assertEquals(0, new BigDecimal("885.60").compareTo(reloaded.getTotalGross()))
            );
        });
    }

    @Test
    void changeItemQuantity_quantityIncreasedAboveStock_conflictReturnedAndQuantityUnchanged() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Cart saved = cartRepository.save(cart);
        Long cartId = saved.getId();
        Long itemId = saved.getItems().getFirst().getId();
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(5);
        stubFor(WireMock.get(urlEqualTo("/inventory?productIds=5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 5, "quantity": 3, "available": true}
                                ]
                                """)));
        //when //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 5. Required: 5, available: 3"));
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=5")));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertEquals(1, cartRepository.findById(cartId).orElseThrow().findItem(itemId).getQuantity()));
    }

    @Test
    void changeItemQuantity_quantityDecreased_itemUpdatedWithoutStockCheck() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 3, List.of());
        Cart saved = cartRepository.save(cart);
        Long cartId = saved.getId();
        Long itemId = saved.getItems().getFirst().getId();
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(1);
        //when //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.total.netPrice").value(240.00))
                .andExpect(jsonPath("$.total.vatAmount").value(55.20))
                .andExpect(jsonPath("$.total.grossPrice").value(295.20));
        verify(0, anyRequestedFor(anyUrl()));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertEquals(1, cartRepository.findById(cartId).orElseThrow().findItem(itemId).getQuantity()));
    }

    @Test
    void removeItem_itemExists_itemDeletedAndTotalsRecalculated() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 1,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        Cart saved = cartRepository.save(cart);
        Long cartId = saved.getId();
        Long headphonesItemId = saved.getItems().get(0).getId();
        Long pcItemId = saved.getItems().get(1).getId();
        //when //then
        mockMvc.perform(delete("/carts/{cartId}/items/{itemId}", cartId, headphonesItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(pcItemId))
                .andExpect(jsonPath("$.items[0].productId").value(1L))
                .andExpect(jsonPath("$.total.netPrice").value(5855.00))
                .andExpect(jsonPath("$.total.vatAmount").value(1346.65))
                .andExpect(jsonPath("$.total.grossPrice").value(7201.65));
        verify(0, anyRequestedFor(anyUrl()));
        transactionTemplate.executeWithoutResult(transaction -> {
            Cart reloaded = cartRepository.findById(cartId).orElseThrow();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1, reloaded.getItems().size()),
                    () -> Assertions.assertEquals(pcItemId, reloaded.getItems().getFirst().getId()),
                    () -> Assertions.assertEquals(1, reloaded.getItems().getFirst().getComponents().size()),
                    () -> Assertions.assertEquals(0, new BigDecimal("7201.65").compareTo(reloaded.getTotalGross()))
            );
        });
    }

    @Test
    void removeItem_itemNotExists_notFoundReturnedAndCartUnchanged() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Long cartId = cartRepository.save(cart).getId();
        //when //then
        mockMvc.perform(delete("/carts/{cartId}/items/{itemId}", cartId, 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CART_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Item with id = 999999 not found in cart with id = " + cartId));
        verify(0, anyRequestedFor(anyUrl()));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertEquals(1, cartRepository.findById(cartId).orElseThrow().getItems().size()));
    }

    @Test
    void getCart_cartWithItems_cartWithComponentsAndTotalsReturned() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 2,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        Cart saved = cartRepository.save(cart);
        Long cartId = saved.getId();
        Long itemId = saved.getItems().getFirst().getId();
        //when //then
        mockMvc.perform(get("/carts/{cartId}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(itemId))
                .andExpect(jsonPath("$.items[0].productName").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice.grossPrice").value(7201.65))
                .andExpect(jsonPath("$.items[0].totalPrice.netPrice").value(11710.00))
                .andExpect(jsonPath("$.items[0].components[0].configurationId").value(11L))
                .andExpect(jsonPath("$.items[0].components[0].componentName").value("RTX 4070"))
                .andExpect(jsonPath("$.total.netPrice").value(11710.00))
                .andExpect(jsonPath("$.total.vatAmount").value(2693.30))
                .andExpect(jsonPath("$.total.grossPrice").value(14403.30))
                .andExpect(jsonPath("$.total.currency").value("PLN"));
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void getCart_cartNotExists_notFoundReturned() throws Exception {
        //given
        Long cartId = 999999L;
        //when //then
        mockMvc.perform(get("/carts/{cartId}", cartId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("CART_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cart with id = 999999 doesn't exist"));
        verify(0, anyRequestedFor(anyUrl()));
    }
}
