package fi.metropolia.nurha.webstore;

import fi.metropolia.nurha.webstore.entity.Product;
import fi.metropolia.nurha.webstore.entity.ProductCategory;
import fi.metropolia.nurha.webstore.repository.ProductCategoryRepository;
import fi.metropolia.nurha.webstore.repository.ProductRepository;
import fi.metropolia.nurha.webstore.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    private ProductCategory testCategory;

    @BeforeEach
    void setUp() {
        // Luo testikategoria
        testCategory = new ProductCategory();
        testCategory.setName("Testikategoria");
        testCategory = categoryRepository.save(testCategory);

        // Luo kolme testituotetta eri hinnoilla
        saveProduct("Halpa tuote", "9.99", testCategory);
        saveProduct("Keskihintainen tuote", "49.99", testCategory);
        saveProduct("Kallis tuote", "199.99", null);  // ei kategoriaa
    }

    private void saveProduct(String name, String price,
                             ProductCategory category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStockQuantity(10);
        p.setCategory(category);
        productRepository.save(p);
    }

    @Test
    void searchProducts_byPriceRange_returnsCorrectProducts() {
        List<Product> results = productService.searchProducts(
                new BigDecimal("5.00"),
                new BigDecimal("50.00"),
                null
        );

        // Pitäisi löytää 2 tuotetta (9.99 ja 49.99)
        assertTrue(results.stream()
                        .allMatch(p -> p.getPrice().compareTo(new BigDecimal("5.00")) >= 0
                                && p.getPrice().compareTo(new BigDecimal("50.00")) <= 0),
                "Kaikkien tuotteiden hinnan pitäisi olla välillä 5-50");
    }

    @Test
    void searchProducts_byCategory_returnsOnlyCategoryProducts() {
        List<Product> results = productService.searchProducts(
                null, null, testCategory.getId()
        );

        // Pitäisi löytää 2 tuotetta (halpa ja keskihintainen)
        assertTrue(results.stream()
                        .allMatch(p -> p.getCategory() != null
                                && p.getCategory().getId()
                                .equals(testCategory.getId())),
                "Kaikkien tuotteiden pitäisi kuulua testikategoriaan");
    }

    @Test
    void searchProducts_noParameters_returnsResults() {
        List<Product> results = productService.searchProducts(
                null, null, null
        );

        // Ilman parametreja pitäisi palauttaa tuloksia
        assertFalse(results.isEmpty(),
                "Tyhjällä haulla pitäisi löytyä tuloksia");
    }

    @Test
    void searchProducts_priceAndCategory_returnsFilteredProducts() {
        List<Product> results = productService.searchProducts(
                new BigDecimal("5.00"),
                new BigDecimal("50.00"),
                testCategory.getId()
        );

        // Pitäisi löytää vain halpa tuote (9.99, oikeassa kategoriassa)
        assertTrue(results.stream()
                        .allMatch(p -> p.getCategory() != null
                                && p.getCategory().getId().equals(testCategory.getId())
                                && p.getPrice().compareTo(new BigDecimal("50.00")) <= 0),
                "Tuotteiden pitäisi täyttää molemmat ehdot");
    }
}