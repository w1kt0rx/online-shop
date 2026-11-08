package pl.shop.product.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.shop.product.dto.request.CreateProductConfigurationRequest;
import pl.shop.product.dto.request.QuoteRequest;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.dto.response.QuoteComponentResponse;
import pl.shop.product.dto.response.QuoteResponse;
import pl.shop.product.kafka.event.ProductDeletedEvent;
import pl.shop.product.model.Price;
import pl.shop.product.model.Product;
import pl.shop.product.model.ProductConfiguration;
import pl.shop.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductConfigurationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM product.product_configurations");
        productRepository.deleteAll();
    }

    @Test
    void createProductConfiguration_dataCorrect_configurationPersisted() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = productRepository.save(new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true));
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(gpu.getId(), "Graphics card", true, false);
        //when //then
        mockMvc.perform(post("/products/{productId}/configurations", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.slot").value("Graphics card"))
                .andExpect(jsonPath("$.componentId").value(gpu.getId()))
                .andExpect(jsonPath("$.componentName").value("RTX 4060"))
                .andExpect(jsonPath("$.defaultComponent").value(true))
                .andExpect(jsonPath("$.multiSelect").value(false));
        mockMvc.perform(get("/products/{id}", pc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurations", hasSize(1)))
                .andExpect(jsonPath("$.configurations[0].componentName").value("RTX 4060"));
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM product.product_configurations WHERE product_id = ?", Integer.class, pc.getId())),
                () -> Assertions.assertEquals(gpu.getId(), jdbcTemplate.queryForObject(
                        "SELECT component_id FROM product.product_configurations WHERE product_id = ?", Long.class, pc.getId())),
                () -> Assertions.assertEquals("Graphics card", jdbcTemplate.queryForObject(
                        "SELECT slot FROM product.product_configurations WHERE product_id = ?", String.class, pc.getId()))
        );
    }

    @Test
    void createProductConfiguration_componentNotExists_returnsNotFoundAndNothingSaved() throws Exception {
        //given
        Product pc = productRepository.save(new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true));
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(999999L, "Graphics card", true, false);
        //when //then
        mockMvc.perform(post("/products/{productId}/configurations", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Couldn't find product with id: 999999"));
        Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product.product_configurations", Integer.class));
    }

    @Test
    void createProductConfiguration_productAsOwnComponent_returnsBadRequestAndNothingSaved() throws Exception {
        //given
        Product pc = productRepository.save(new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true));
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(pc.getId(), "Graphics card", true, false);
        //when //then
        mockMvc.perform(post("/products/{productId}/configurations", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CONFIGURATION"))
                .andExpect(jsonPath("$.message").value("Product cannot be its own component"));
        Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product.product_configurations", Integer.class));
    }

    @Test
    void createProductConfiguration_secondDefaultInSlot_returnsBadRequestAndNothingSaved() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        Product otherGpu = productRepository.save(new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2400.00"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true);
        pc.addConfiguration(gpu, "Graphics card", true, false);
        productRepository.save(pc);
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(otherGpu.getId(), "Graphics card", true, false);
        //when //then
        mockMvc.perform(post("/products/{productId}/configurations", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CONFIGURATION"))
                .andExpect(jsonPath("$.message").value("Slot already has a default component: Graphics card"));
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM product.product_configurations WHERE product_id = ?", Integer.class, pc.getId())),
                () -> Assertions.assertEquals(gpu.getId(), jdbcTemplate.queryForObject(
                        "SELECT component_id FROM product.product_configurations WHERE product_id = ?", Long.class, pc.getId()))
        );
    }

    @Test
    void getProductConfigurations_configurationsExist_returnsConfigurations() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        Product ram = productRepository.save(new Product("RAM 16 GB DDR5", "Electronics", "Kingston",
                new Price(new BigDecimal("203.00"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true);
        ProductConfiguration gpuConfiguration = pc.addConfiguration(gpu, "Graphics card", true, false);
        ProductConfiguration ramConfiguration = pc.addConfiguration(ram, "RAM", false, true);
        productRepository.save(pc);
        //when //then
        MvcResult result = mockMvc.perform(get("/products/{productId}/configurations", pc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();
        List<ProductConfigurationResponse> responses = List.of(objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductConfigurationResponse[].class));
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, responses.size()),
                () -> Assertions.assertTrue(responses.contains(new ProductConfigurationResponse(
                        gpuConfiguration.getId(), "Graphics card", gpu.getId(), "RTX 4060", true, false))),
                () -> Assertions.assertTrue(responses.contains(new ProductConfigurationResponse(
                        ramConfiguration.getId(), "RAM", ram.getId(), "RAM 16 GB DDR5", false, true)))
        );
    }

    @Test
    void getProductConfigurations_productNotExists_returnsNotFound() throws Exception {
        //given
        Long productId = 999999L;
        //when //then
        mockMvc.perform(get("/products/{productId}/configurations", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Couldn't find product with id: 999999"));
    }

    @Test
    void deleteProductConfiguration_configurationExists_configurationRemovedAndComponentKept() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true);
        ProductConfiguration gpuConfiguration = pc.addConfiguration(gpu, "Graphics card", true, false);
        productRepository.save(pc);
        //when //then
        mockMvc.perform(delete("/products/{productId}/configurations/{configurationId}", pc.getId(), gpuConfiguration.getId()))
                .andExpect(status().isNoContent());
        Assertions.assertAll(
                () -> Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM product.product_configurations", Integer.class)),
                () -> Assertions.assertTrue(productRepository.existsById(pc.getId())),
                () -> Assertions.assertTrue(productRepository.existsById(gpu.getId()))
        );
    }

    @Test
    void deleteProductConfiguration_configurationNotExists_returnsNotFound() throws Exception {
        //given
        Product pc = productRepository.save(new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true));
        //when //then
        mockMvc.perform(delete("/products/{productId}/configurations/{configurationId}", pc.getId(), 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONFIGURATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Configuration not found: id = 999999"));
    }

    @Test
    void quote_noConfigurationsSelected_defaultComponentsAndBasePriceReturned() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1219.99"), new BigDecimal("0.23"), "PLN"), true));
        Product betterGpu = productRepository.save(new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2399.49"), new BigDecimal("0.23"), "PLN"), true));
        Product ram = productRepository.save(new Product("RAM 16 GB DDR5", "Electronics", "Kingston",
                new Price(new BigDecimal("203.00"), new BigDecimal("0.23"), "PLN"), true));
        Product biggerRam = productRepository.save(new Product("RAM 32 GB DDR5", "Electronics", "Kingston",
                new Price(new BigDecimal("389.90"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("3999.99"), new BigDecimal("0.23"), "PLN"), true);
        ProductConfiguration gpuConfiguration = pc.addConfiguration(gpu, "Graphics card", true, false);
        pc.addConfiguration(betterGpu, "Graphics card", false, false);
        ProductConfiguration ramConfiguration = pc.addConfiguration(ram, "RAM", true, false);
        pc.addConfiguration(biggerRam, "RAM", false, false);
        productRepository.save(pc);
        QuoteRequest request = new QuoteRequest(List.of());
        //when //then
        MvcResult result = mockMvc.perform(post("/products/{productId}/configurations/quote", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        QuoteResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), QuoteResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(pc.getId(), response.productId()),
                () -> Assertions.assertEquals("Gaming PC Pro", response.productName()),
                () -> Assertions.assertEquals(2, response.selectedComponents().size()),
                () -> Assertions.assertTrue(response.selectedComponents().contains(new QuoteComponentResponse(
                        gpuConfiguration.getId(), gpu.getId(), "Graphics card", "RTX 4060"))),
                () -> Assertions.assertTrue(response.selectedComponents().contains(new QuoteComponentResponse(
                        ramConfiguration.getId(), ram.getId(), "RAM", "RAM 16 GB DDR5"))),
                () -> Assertions.assertEquals(0, new BigDecimal("3999.99").compareTo(response.unitPrice().netPrice())),
                () -> Assertions.assertEquals(0, new BigDecimal("0.23").compareTo(response.unitPrice().vatRate())),
                () -> Assertions.assertEquals(0, new BigDecimal("920.00").compareTo(response.unitPrice().vatAmount())),
                () -> Assertions.assertEquals(0, new BigDecimal("4919.99").compareTo(response.unitPrice().grossPrice())),
                () -> Assertions.assertEquals("PLN", response.unitPrice().currency()),
                () -> Assertions.assertEquals(3, response.stockProductIds().size()),
                () -> Assertions.assertEquals(pc.getId(), response.stockProductIds().getFirst()),
                () -> Assertions.assertTrue(response.stockProductIds().containsAll(List.of(gpu.getId(), ram.getId())))
        );
    }

    @Test
    void quote_configurationsSelected_priceIncludesDifferencesToDefaults() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1219.99"), new BigDecimal("0.23"), "PLN"), true));
        Product betterGpu = productRepository.save(new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2399.49"), new BigDecimal("0.23"), "PLN"), true));
        Product ram = productRepository.save(new Product("RAM 16 GB DDR5", "Electronics", "Kingston",
                new Price(new BigDecimal("203.00"), new BigDecimal("0.23"), "PLN"), true));
        Product biggerRam = productRepository.save(new Product("RAM 32 GB DDR5", "Electronics", "Kingston",
                new Price(new BigDecimal("389.90"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("3999.99"), new BigDecimal("0.23"), "PLN"), true);
        pc.addConfiguration(gpu, "Graphics card", true, false);
        ProductConfiguration betterGpuConfiguration = pc.addConfiguration(betterGpu, "Graphics card", false, false);
        pc.addConfiguration(ram, "RAM", true, false);
        ProductConfiguration biggerRamConfiguration = pc.addConfiguration(biggerRam, "RAM", false, false);
        productRepository.save(pc);
        QuoteRequest request = new QuoteRequest(List.of(betterGpuConfiguration.getId(), biggerRamConfiguration.getId()));
        //when //then
        MvcResult result = mockMvc.perform(post("/products/{productId}/configurations/quote", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        QuoteResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), QuoteResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(pc.getId(), response.productId()),
                () -> Assertions.assertEquals(2, response.selectedComponents().size()),
                () -> Assertions.assertTrue(response.selectedComponents().contains(new QuoteComponentResponse(
                        betterGpuConfiguration.getId(), betterGpu.getId(), "Graphics card", "RTX 4070"))),
                () -> Assertions.assertTrue(response.selectedComponents().contains(new QuoteComponentResponse(
                        biggerRamConfiguration.getId(), biggerRam.getId(), "RAM", "RAM 32 GB DDR5"))),
                () -> Assertions.assertEquals(0, new BigDecimal("5366.39").compareTo(response.unitPrice().netPrice())),
                () -> Assertions.assertEquals(0, new BigDecimal("1234.27").compareTo(response.unitPrice().vatAmount())),
                () -> Assertions.assertEquals(0, new BigDecimal("6600.66").compareTo(response.unitPrice().grossPrice())),
                () -> Assertions.assertEquals("PLN", response.unitPrice().currency()),
                () -> Assertions.assertEquals(3, response.stockProductIds().size()),
                () -> Assertions.assertEquals(pc.getId(), response.stockProductIds().getFirst()),
                () -> Assertions.assertTrue(response.stockProductIds().containsAll(List.of(betterGpu.getId(), biggerRam.getId())))
        );
    }

    @Test
    void quote_twoConfigurationsInSingleSelectSlot_returnsBadRequest() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        Product betterGpu = productRepository.save(new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2400.00"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true);
        ProductConfiguration gpuConfiguration = pc.addConfiguration(gpu, "Graphics card", true, false);
        ProductConfiguration betterGpuConfiguration = pc.addConfiguration(betterGpu, "Graphics card", false, false);
        productRepository.save(pc);
        QuoteRequest request = new QuoteRequest(List.of(gpuConfiguration.getId(), betterGpuConfiguration.getId()));
        //when //then
        mockMvc.perform(post("/products/{productId}/configurations/quote", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CONFIGURATION"))
                .andExpect(jsonPath("$.message").value("Only one component can be selected in slot: Graphics card"));
    }

    @Test
    void quote_configurationNotBelongingToProduct_returnsBadRequest() throws Exception {
        //given
        Product pc = productRepository.save(new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true));
        QuoteRequest request = new QuoteRequest(List.of(999999L));
        //when //then
        mockMvc.perform(post("/products/{productId}/configurations/quote", pc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CONFIGURATION"))
                .andExpect(jsonPath("$.message").value("Configurations [999999] do not belong to product id = " + pc.getId()));
    }

    @Test
    void deleteProduct_productUsedAsComponent_returnsConflictAndProductKept() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true);
        pc.addConfiguration(gpu, "Graphics card", true, false);
        productRepository.save(pc);
        //when //then
        mockMvc.perform(delete("/products/{id}", gpu.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Operation violates data constraints"));
        Assertions.assertAll(
                () -> Assertions.assertTrue(productRepository.existsById(gpu.getId())),
                () -> Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM product.product_configurations WHERE component_id = ?", Integer.class, gpu.getId()))
        );
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void deleteProduct_productWithConfigurations_productAndConfigurationsRemovedAndEventSent() throws Exception {
        //given
        Product gpu = productRepository.save(new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true));
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4000.00"), new BigDecimal("0.23"), "PLN"), true);
        pc.addConfiguration(gpu, "Graphics card", true, false);
        productRepository.save(pc);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when //then
        mockMvc.perform(delete("/products/{id}", pc.getId()))
                .andExpect(status().isNoContent());
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertFalse(productRepository.existsById(pc.getId())),
                () -> Assertions.assertTrue(productRepository.existsById(gpu.getId())),
                () -> Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM product.product_configurations", Integer.class)),
                () -> Assertions.assertEquals("product-deleted", topicCaptor.getValue()),
                () -> Assertions.assertEquals(String.valueOf(pc.getId()), keyCaptor.getValue()),
                () -> Assertions.assertEquals(new ProductDeletedEvent(pc.getId(), "Gaming PC Pro"), eventCaptor.getValue())
        );
    }
}
