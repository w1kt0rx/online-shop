package pl.shop.interaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import pl.shop.interaction.dto.response.CartItemResponse;
import pl.shop.interaction.dto.response.CartResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
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

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void createCart_cartServiceReturns201_createdCartReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/carts"))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 1, "status": "ACTIVE", "items": [],
                                 "total": {"netPrice": 0, "vatAmount": 0, "grossPrice": 0, "currency": "PLN"}}
                                """)));
        //when //then
        mockMvc.perform(post("/api/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total.currency").value("PLN"));
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/carts")));
    }

    @Test
    void addItem_dataCorrect_exactBodyForwardedAndCartReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/carts/1/items"))
                .withRequestBody(WireMock.equalToJson("""
                        {"productId": 5, "configurationIds": [2, 4], "quantity": 2}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 1, "status": "ACTIVE",
                                 "items": [{"id": 100, "productId": 5, "productName": "Gaming PC Pro", "quantity": 2,
                                            "unitPrice": {"netPrice": 6042.00, "vatRate": 0.23, "vatAmount": 1389.66, "grossPrice": 7431.66, "currency": "PLN"},
                                            "totalPrice": {"netPrice": 12084.00, "vatRate": 0.23, "vatAmount": 2779.32, "grossPrice": 14863.32, "currency": "PLN"},
                                            "components": [{"configurationId": 2, "componentProductId": 2, "slot": "Graphics card", "componentName": "RTX 4070"},
                                                           {"configurationId": 4, "componentProductId": 4, "slot": "RAM", "componentName": "32GB DDR5"}]}],
                                 "total": {"netPrice": 12084.00, "vatAmount": 2779.32, "grossPrice": 14863.32, "currency": "PLN"}}
                                """)));
        //when
        String json = mockMvc.perform(post("/api/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 5, "configurationIds": [2, 4], "quantity": 2}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        CartResponse response = objectMapper.readValue(json, CartResponse.class);
        CartItemResponse item = response.items().getFirst();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals(1, response.items().size()),
                () -> Assertions.assertEquals(100L, item.id()),
                () -> Assertions.assertEquals(2, item.quantity()),
                () -> Assertions.assertEquals(2, item.components().size()),
                () -> Assertions.assertEquals(0, new BigDecimal("14863.32").compareTo(response.total().grossPrice()))
        );
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/carts/1/items"))
                .withRequestBody(WireMock.equalToJson("""
                        {"productId": 5, "configurationIds": [2, 4], "quantity": 2}
                        """)));
    }

    @Test
    void addItem_configurationIdsMissing_emptyListForwarded() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/carts/1/items"))
                .withRequestBody(WireMock.equalToJson("""
                        {"productId": 2, "configurationIds": [], "quantity": 1}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 1, "status": "ACTIVE", "items": []}
                                """)));
        //when //then
        mockMvc.perform(post("/api/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 2, "quantity": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/carts/1/items"))
                .withRequestBody(WireMock.equalToJson("""
                        {"productId": 2, "configurationIds": [], "quantity": 1}
                        """)));
    }

    @Test
    void addItem_cartServiceReturns409InsufficientStock_conflictPassedThroughWithoutRetry() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/carts/1/items"))
                .willReturn(WireMock.aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 409, "errorCode": "INSUFFICIENT_STOCK", "message": "Insufficient stock for product with id = 4. Required: 2, available: 1"}
                                """)));
        //when //then
        mockMvc.perform(post("/api/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": 5, "configurationIds": [2, 4], "quantity": 2}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 4. Required: 2, available: 1"));
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/carts/1/items")));
    }

    @Test
    void changeItemQuantity_dataCorrect_patchWithBodyArrivesAtCartService() throws Exception {
        //given
        WireMock.stubFor(WireMock.patch(WireMock.urlEqualTo("/carts/1/items/100"))
                .withRequestBody(WireMock.equalToJson("""
                        {"quantity": 3}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 1, "status": "ACTIVE",
                                 "items": [{"id": 100, "productId": 2, "productName": "RTX 4070", "quantity": 3, "components": []}]}
                                """)));
        //when
        String json = mockMvc.perform(patch("/api/carts/{cartId}/items/{itemId}", 1L, 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 3}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        CartResponse response = objectMapper.readValue(json, CartResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals(100L, response.items().getFirst().id()),
                () -> Assertions.assertEquals(3, response.items().getFirst().quantity())
        );
        WireMock.verify(1, WireMock.patchRequestedFor(WireMock.urlEqualTo("/carts/1/items/100"))
                .withRequestBody(WireMock.equalToJson("""
                        {"quantity": 3}
                        """)));
    }

    @Test
    void createCart_cartServiceReturns500_serviceUnavailableReturnedWithoutRetry() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/carts"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 500, "errorCode": "INTERNAL_ERROR", "message": "Unexpected error occurred"}
                                """)));
        //when //then
        mockMvc.perform(post("/api/carts"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("REMOTE_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("Remote service failed, try again later"));
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/carts")));
    }

    @Test
    void addItem_bodyMalformed_badRequestAndNoDownstreamCall() throws Exception {
        //given
        String body = "{\"productId\": 5, \"quantity\":";
        //when //then
        mockMvc.perform(post("/api/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is invalid or malformed"));
        WireMock.verify(0, WireMock.anyRequestedFor(WireMock.anyUrl()));
    }
}
