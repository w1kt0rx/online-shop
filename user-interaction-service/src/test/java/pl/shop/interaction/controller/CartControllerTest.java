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
import pl.shop.interaction.dto.request.AddCartItemRequest;
import pl.shop.interaction.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.interaction.dto.response.CartResponse;
import pl.shop.interaction.dto.response.CartTotalResponse;
import pl.shop.interaction.service.CartService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CartService cartService;

    @Test
    void createCart_noBody_returnsCreatedCart() throws Exception {
        //given
        when(cartService.createCart()).thenReturn(new CartResponse(1L, "ACTIVE", List.of(),
                new CartTotalResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN")));
        //when //then
        mockMvc.perform(post("/api/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getCart_cartExists_returnsCart() throws Exception {
        //given
        when(cartService.getCart(1L)).thenReturn(new CartResponse(1L, "ORDERED", List.of(), null));
        //when //then
        mockMvc.perform(get("/api/carts/{cartId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"));
    }

    @Test
    void addItem_dataCorrect_requestPassedAndCartReturned() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(2L, 4L), 1);
        ArgumentCaptor<AddCartItemRequest> requestCaptor = ArgumentCaptor.forClass(AddCartItemRequest.class);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class))).thenReturn(new CartResponse(1L, "ACTIVE", List.of(), null));
        //when //then
        mockMvc.perform(post("/api/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        Mockito.verify(cartService).addItem(Mockito.eq(1L), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(5L, requestCaptor.getValue().productId()),
                () -> Assertions.assertEquals(List.of(2L, 4L), requestCaptor.getValue().configurationIds())
        );
    }

    @Test
    void addItem_insufficientStockInCartService_returnsConflictWithRemoteError() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.POST, "http://localhost:8083/carts/1/items", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":409,\"errorCode\":\"INSUFFICIENT_STOCK\",\"message\":\"Insufficient stock for product with id = 4. Required: 2, available: 1\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new FeignException.Conflict("Conflict", request, body, null));
        //when //then
        mockMvc.perform(post("/api/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":5,\"configurationIds\":[2,4],\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 4. Required: 2, available: 1"));
    }

    @Test
    void addItem_validationFailedInCartService_returnsBadRequestWithRemoteError() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.POST, "http://localhost:8083/carts/1/items", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":400,\"errorCode\":\"ARGUMENT_NOT_VALID\",\"message\":\"Validation failed: quantity: Quantity cannot exceed 99\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new FeignException.BadRequest("Bad Request", request, body, null));
        //when //then
        mockMvc.perform(post("/api/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":5,\"quantity\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"));
    }

    @Test
    void changeItemQuantity_dataCorrect_requestPassed() throws Exception {
        //given
        ArgumentCaptor<ChangeCartItemQuantityRequest> requestCaptor = ArgumentCaptor.forClass(ChangeCartItemQuantityRequest.class);
        when(cartService.changeItemQuantity(anyLong(), anyLong(), any(ChangeCartItemQuantityRequest.class)))
                .thenReturn(new CartResponse(1L, "ACTIVE", List.of(), null));
        //when //then
        mockMvc.perform(patch("/api/carts/{cartId}/items/{itemId}", 1L, 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk());
        Mockito.verify(cartService).changeItemQuantity(Mockito.eq(1L), Mockito.eq(100L), requestCaptor.capture());
        Assertions.assertEquals(3, requestCaptor.getValue().quantity());
    }

    @Test
    void removeItem_idsGiven_returnsCart() throws Exception {
        //given
        when(cartService.removeItem(1L, 100L)).thenReturn(new CartResponse(1L, "ACTIVE", List.of(), null));
        //when //then
        mockMvc.perform(delete("/api/carts/{cartId}/items/{itemId}", 1L, 100L))
                .andExpect(status().isOk());
        Mockito.verify(cartService).removeItem(1L, 100L);
    }
}
