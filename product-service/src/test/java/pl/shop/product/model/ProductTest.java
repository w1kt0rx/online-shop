package pl.shop.product.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.product.exception.ConfigurationNotFoundException;
import pl.shop.product.exception.InvalidConfigurationException;

import java.math.BigDecimal;
import java.util.List;

public class ProductTest {

    @Test
    void addConfiguration_dataCorrect_configurationAdded() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 6L);
        //when
        ProductConfiguration configuration = pc.addConfiguration(gpu, "Graphics card", true, false);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, pc.getConfigurations().size()),
                () -> Assertions.assertSame(configuration, pc.getConfigurations().getFirst()),
                () -> Assertions.assertSame(pc, configuration.getProduct()),
                () -> Assertions.assertSame(gpu, configuration.getComponent()),
                () -> Assertions.assertEquals("Graphics card", configuration.getSlot()),
                () -> Assertions.assertTrue(configuration.isDefaultComponent()),
                () -> Assertions.assertFalse(configuration.isMultiSelect()),
                () -> Assertions.assertTrue(pc.isConfigurable())
        );
    }

    @Test
    void addConfiguration_componentIsSameProduct_throwsInvalidConfigurationException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        //when //then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> pc.addConfiguration(pc, "Graphics card", true, false)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Product cannot be its own component", ex.getMessage()),
                () -> Assertions.assertTrue(pc.getConfigurations().isEmpty())
        );
    }

    @Test
    void addConfiguration_componentAlreadyInSlot_throwsInvalidConfigurationException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 6L);
        pc.addConfiguration(gpu, "Graphics card", true, false);
        //when //then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> pc.addConfiguration(gpu, "Graphics card", false, false)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Component already added to slot: Graphics card", ex.getMessage()),
                () -> Assertions.assertEquals(1, pc.getConfigurations().size())
        );
    }

    @Test
    void addConfiguration_secondDefaultInSlot_throwsInvalidConfigurationException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product rtx4060 = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4060, "id", 6L);
        Product rtx4070 = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4070, "id", 4L);
        pc.addConfiguration(rtx4060, "Graphics card", true, false);
        //when //then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> pc.addConfiguration(rtx4070, "Graphics card", true, false)
        );
        Assertions.assertEquals("Slot already has a default component: Graphics card", ex.getMessage());
    }

    @Test
    void addConfiguration_multiSelectInconsistentInSlot_throwsInvalidConfigurationException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product ssd = new Product("SSD 1 TB", "Electronics", "Samsung",
                new Price(new BigDecimal("300.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(ssd, "id", 8L);
        Product hdd = new Product("HDD 2 TB", "Electronics", "Seagate",
                new Price(new BigDecimal("250.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(hdd, "id", 9L);
        pc.addConfiguration(ssd, "Storage", false, true);
        //when //then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> pc.addConfiguration(hdd, "Storage", false, false)
        );
        Assertions.assertEquals("multiSelect must be the same for whole slot: Storage", ex.getMessage());
    }

    @Test
    void removeConfiguration_configurationExists_configurationRemoved() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 6L);
        ProductConfiguration configuration = pc.addConfiguration(gpu, "Graphics card", true, false);
        ReflectionTestUtils.setField(configuration, "id", 10L);
        //when
        pc.removeConfiguration(10L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertTrue(pc.getConfigurations().isEmpty()),
                () -> Assertions.assertFalse(pc.isConfigurable())
        );
    }

    @Test
    void removeConfiguration_configurationNotExists_throwsConfigurationNotFoundException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        //when //then
        ConfigurationNotFoundException ex = Assertions.assertThrows(
                ConfigurationNotFoundException.class,
                () -> pc.removeConfiguration(999L)
        );
        Assertions.assertEquals("Configuration not found: id = 999", ex.getMessage());
    }

    @Test
    void getConfigurations_listModified_throwsUnsupportedOperationException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        //when //then
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> pc.getConfigurations().clear()
        );
    }

    @Test
    void quote_productWithoutConfigurations_basePriceAndOnlyProductIdReturned() {
        //given
        Product headphones = new Product("Headphones ABC", "Electronics", "Sony",
                new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(headphones, "id", 5L);
        //when
        Quote quote = headphones.quote(List.of());
        //then
        Assertions.assertAll(
                () -> Assertions.assertTrue(quote.selectedConfigurations().isEmpty()),
                () -> Assertions.assertEquals(new BigDecimal("240.00"), quote.unitPrice().getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("295.20"), quote.unitPrice().getGrossPrice()),
                () -> Assertions.assertEquals(List.of(5L), quote.stockProductIds())
        );
    }

    @Test
    void quote_nothingRequested_defaultsSelectedAndBasePriceReturned() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product rtx4060 = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4060, "id", 6L);
        Product rtx4070 = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4070, "id", 4L);
        Product ram = new Product("RAM 16 GB DDR5", "Electronics", "Kingston",
                new Price(new BigDecimal("203.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(ram, "id", 7L);
        ProductConfiguration defaultGpu = pc.addConfiguration(rtx4060, "Graphics card", true, false);
        ReflectionTestUtils.setField(defaultGpu, "id", 10L);
        ProductConfiguration upgradeGpu = pc.addConfiguration(rtx4070, "Graphics card", false, false);
        ReflectionTestUtils.setField(upgradeGpu, "id", 11L);
        ProductConfiguration defaultRam = pc.addConfiguration(ram, "RAM", true, false);
        ReflectionTestUtils.setField(defaultRam, "id", 12L);
        //when
        Quote quote = pc.quote(List.of());
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(defaultGpu, defaultRam), quote.selectedConfigurations()),
                () -> Assertions.assertEquals(new BigDecimal("4800.00"), quote.unitPrice().getNetPrice()),
                () -> Assertions.assertEquals(List.of(1L, 6L, 7L), quote.stockProductIds())
        );
    }

    @Test
    void quote_upgradeRequested_priceIncreasedByDifferenceToDefault() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product rtx4060 = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4060, "id", 6L);
        Product rtx4070 = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4070, "id", 4L);
        ProductConfiguration defaultGpu = pc.addConfiguration(rtx4060, "Graphics card", true, false);
        ReflectionTestUtils.setField(defaultGpu, "id", 10L);
        ProductConfiguration upgradeGpu = pc.addConfiguration(rtx4070, "Graphics card", false, false);
        ReflectionTestUtils.setField(upgradeGpu, "id", 11L);
        //when
        Quote quote = pc.quote(List.of(11L));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(upgradeGpu), quote.selectedConfigurations()),
                () -> Assertions.assertEquals(new BigDecimal("5855.00"), quote.unitPrice().getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("7201.65"), quote.unitPrice().getGrossPrice()),
                () -> Assertions.assertEquals(List.of(1L, 4L), quote.stockProductIds())
        );
    }

    @Test
    void quote_cheaperOptionRequested_priceDecreasedByDifferenceToDefault() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product rtx4060 = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4060, "id", 6L);
        Product rtx4070 = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4070, "id", 4L);
        ProductConfiguration defaultGpu = pc.addConfiguration(rtx4070, "Graphics card", true, false);
        ReflectionTestUtils.setField(defaultGpu, "id", 10L);
        ProductConfiguration cheaperGpu = pc.addConfiguration(rtx4060, "Graphics card", false, false);
        ReflectionTestUtils.setField(cheaperGpu, "id", 11L);
        //when
        Quote quote = pc.quote(List.of(11L));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(cheaperGpu), quote.selectedConfigurations()),
                () -> Assertions.assertEquals(new BigDecimal("3745.00"), quote.unitPrice().getNetPrice()),
                () -> Assertions.assertEquals(List.of(1L, 6L), quote.stockProductIds())
        );
    }

    @Test
    void quote_optionalSlotWithoutDefaultRequested_priceIncreasedByComponentPrice() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product ssd = new Product("SSD 1 TB", "Electronics", "Samsung",
                new Price(new BigDecimal("300.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(ssd, "id", 8L);
        Product hdd = new Product("HDD 2 TB", "Electronics", "Seagate",
                new Price(new BigDecimal("250.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(hdd, "id", 9L);
        ProductConfiguration ssdConfiguration = pc.addConfiguration(ssd, "Extra storage", false, true);
        ReflectionTestUtils.setField(ssdConfiguration, "id", 20L);
        ProductConfiguration hddConfiguration = pc.addConfiguration(hdd, "Extra storage", false, true);
        ReflectionTestUtils.setField(hddConfiguration, "id", 21L);
        //when
        Quote quote = pc.quote(List.of(20L, 21L));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(ssdConfiguration, hddConfiguration), quote.selectedConfigurations()),
                () -> Assertions.assertEquals(new BigDecimal("5350.00"), quote.unitPrice().getNetPrice()),
                () -> Assertions.assertEquals(List.of(1L, 8L, 9L), quote.stockProductIds())
        );
    }

    @Test
    void quote_twoOptionsInSingleSelectSlot_throwsInvalidConfigurationException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product rtx4060 = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4060, "id", 6L);
        Product rtx4070 = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(rtx4070, "id", 4L);
        ProductConfiguration defaultGpu = pc.addConfiguration(rtx4060, "Graphics card", true, false);
        ReflectionTestUtils.setField(defaultGpu, "id", 10L);
        ProductConfiguration upgradeGpu = pc.addConfiguration(rtx4070, "Graphics card", false, false);
        ReflectionTestUtils.setField(upgradeGpu, "id", 11L);
        //when //then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> pc.quote(List.of(10L, 11L))
        );
        Assertions.assertEquals("Only one component can be selected in slot: Graphics card", ex.getMessage());
    }

    @Test
    void quote_configurationNotBelongingToProduct_throwsInvalidConfigurationException() {
        //given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 6L);
        ProductConfiguration configuration = pc.addConfiguration(gpu, "Graphics card", true, false);
        ReflectionTestUtils.setField(configuration, "id", 10L);
        //when //then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> pc.quote(List.of(10L, 77L, 55L))
        );
        Assertions.assertEquals("Configurations [55, 77] do not belong to product id = 1", ex.getMessage());
    }

    @Test
    void isConfigurable_noConfigurations_returnsFalse() {
        //given
        Product headphones = new Product("Headphones ABC", "Electronics", "Sony",
                new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), true);
        //when
        boolean configurable = headphones.isConfigurable();
        //then
        Assertions.assertFalse(configurable);
    }
}
