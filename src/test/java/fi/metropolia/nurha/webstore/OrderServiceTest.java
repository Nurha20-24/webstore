package fi.metropolia.nurha.webstore;

import fi.metropolia.nurha.webstore.dto.CreateOrderRequest;
import fi.metropolia.nurha.webstore.dto.OrderItemRequest;
import fi.metropolia.nurha.webstore.entity.*;
import fi.metropolia.nurha.webstore.repository.*;
import fi.metropolia.nurha.webstore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// @SpringBootTest lataa koko Spring-kontekstin
// @Transactional peruuttaa jokaisen testin muutokset automaattisesti
// testit eivät vaikuta toisiinsa
@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Customer testCustomer;
    private Product testProduct;

    // @BeforeEach ajetaan ennen jokaista testiä
    // Luodaan testitietokantaan asiakas ja tuote
    @BeforeEach
    void setUp() {
        // Luo testikategoria
        ProductCategory category = new ProductCategory();
        category.setName("Testikategoria");

        // Luo testituote
        testProduct = new Product();
        testProduct.setName("Testituote");
        testProduct.setPrice(new BigDecimal("29.99"));
        testProduct.setStockQuantity(10);
        testProduct = productRepository.save(testProduct);

        // Luo testiasiakas
        testCustomer = new Customer();
        testCustomer.setFirst_name("Testi");
        testCustomer.setLast_name("Käyttäjä");
        testCustomer.setEmail("testi" + System.currentTimeMillis() + "@example.fi");
        testCustomer = customerRepository.save(testCustomer);
    }

    // Testi 1: onnistunut tilaus
    @Test
    void createOrder_success() {
        // Valmistelu: luodaan pyyntö
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(testProduct.getId());
        item.setQuantity(3);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(testCustomer.getId());
        request.setItems(List.of(item));

        // Toiminto: luodaan tilaus
        Order order = orderService.createOrder(request);

        // Tarkistukset
        assertNotNull(order.getId(),
                "Tilauksen ID:n pitäisi olla asetettu");
        assertEquals("NEW", order.getStatus(),
                "Tilauksen statuksen pitäisi olla NEW");
        assertEquals(1, order.getOrderItems().size(),
                "Tilauksessa pitäisi olla 1 tilausrivi");

        // Tarkista että varasto väheni
        Product updatedProduct = productRepository
                .findById(testProduct.getId()).orElseThrow();
        assertEquals(7, updatedProduct.getStockQuantity(),
                "Varaston pitäisi olla 10 - 3 = 7");
    }

    // Testi 2: varasto ei riitä → rollback
    @Test
    void createOrder_notEnoughStock_throwsException() {
        // Valmistelu: pyydetään enemmän kuin varastossa on (10 kpl)
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(testProduct.getId());
        item.setQuantity(99);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(testCustomer.getId());
        request.setItems(List.of(item));

        // Toiminto + tarkistus: pitäisi heittää poikkeus
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder(request),
                "Pitäisi heittää RuntimeException kun varasto ei riitä"
        );

        assertTrue(exception.getMessage().contains("Not enough stock"),
                "Virheviestin pitäisi sisältää 'Not enough stock'");

        // Tarkista että varasto EI muuttunut (rollback toimi)
        Product unchangedProduct = productRepository
                .findById(testProduct.getId()).orElseThrow();
        assertEquals(10, unchangedProduct.getStockQuantity(),
                "Varaston pitäisi olla edelleen 10 — rollback toimi");
    }

    // Testi 3: tuotetta ei löydy → rollback
    @Test
    void createOrder_productNotFound_throwsException() {
        // Valmistelu: olematon tuote-ID
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(99999);
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(testCustomer.getId());
        request.setItems(List.of(item));

        // Toiminto + tarkistus
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder(request),
                "Pitäisi heittää RuntimeException kun tuotetta ei löydy"
        );

        assertTrue(exception.getMessage().contains("Product not found"),
                "Virheviestin pitäisi sisältää 'Product not found'");
    }

    // Testi 4: asiakas ei löydy
    @Test
    void createOrder_customerNotFound_throwsException() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(testProduct.getId());
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(99999);  // olematon asiakas
        request.setItems(List.of(item));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder(request)
        );

        assertTrue(exception.getMessage().contains("Customer not found"));
    }
}