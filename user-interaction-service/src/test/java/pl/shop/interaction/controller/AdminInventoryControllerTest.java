package pl.shop.interaction.controller;

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
import pl.shop.interaction.dto.request.ChangeQuantityRequest;
import pl.shop.interaction.dto.request.DeliveryRequest;
import pl.shop.interaction.dto.response.StockResponse;
import pl.shop.interaction.service.AdminInventoryService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminInventoryControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdminInventoryService adminInventoryService;

    @Test
    void getStock_productGiven_returnsStock() throws Exception {
        //given
        when(adminInventoryService.getStock(5L)).thenReturn(new StockResponse(5L, 10, true));
        //when //then
        mockMvc.perform(get("/api/admin/inventory/{productId}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStocks_productIdsGiven_passedToService() throws Exception {
        //given
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        when(adminInventoryService.getStocks(any())).thenReturn(List.of(new StockResponse(1L, 5, true), new StockResponse(2L, 2, true)));
        //when //then
        mockMvc.perform(get("/api/admin/inventory").param("productIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        Mockito.verify(adminInventoryService).getStocks(idsCaptor.capture());
        Assertions.assertEquals(List.of(1L, 2L), idsCaptor.getValue());
    }

    @Test
    void getStocks_productIdsMissing_returnsBadRequest() throws Exception {
        //given
        String url = "/api/admin/inventory";
        //when //then
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
    }

    @Test
    void changeQuantity_dataCorrect_requestPassed() throws Exception {
        //given
        ArgumentCaptor<ChangeQuantityRequest> requestCaptor = ArgumentCaptor.forClass(ChangeQuantityRequest.class);
        when(adminInventoryService.changeQuantity(anyLong(), any(ChangeQuantityRequest.class))).thenReturn(new StockResponse(5L, 10, true));
        //when //then
        mockMvc.perform(put("/api/admin/inventory/{productId}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().isOk());
        Mockito.verify(adminInventoryService).changeQuantity(Mockito.eq(5L), requestCaptor.capture());
        Assertions.assertEquals(10, requestCaptor.getValue().quantity());
    }

    @Test
    void registerDelivery_dataCorrect_requestPassed() throws Exception {
        //given
        ArgumentCaptor<DeliveryRequest> requestCaptor = ArgumentCaptor.forClass(DeliveryRequest.class);
        when(adminInventoryService.registerDelivery(anyLong(), any(DeliveryRequest.class))).thenReturn(new StockResponse(4L, 6, true));
        //when //then
        mockMvc.perform(post("/api/admin/inventory/{productId}/deliveries", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(6));
        Mockito.verify(adminInventoryService).registerDelivery(Mockito.eq(4L), requestCaptor.capture());
        Assertions.assertEquals(5, requestCaptor.getValue().quantity());
    }
}
