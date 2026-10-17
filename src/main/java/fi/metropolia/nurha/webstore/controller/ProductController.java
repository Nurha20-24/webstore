package fi.metropolia.nurha.webstore.controller;


import fi.metropolia.nurha.webstore.entity.Product;
import fi.metropolia.nurha.webstore.repository.ProductRepository;
import fi.metropolia.nurha.webstore.service.ProductService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    private final ProductService productService;

    public ProductController(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    // GET /api/products
    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Integer id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return productRepository.save(product);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Integer id, @RequestBody Product updated) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(updated.getName());
                    product.setDescription(updated.getDescription());
                    product.setPrice(updated.getPrice());
                    product.setStockQuantity(updated.getStockQuantity());
                    return ResponseEntity.ok(productRepository.save(product));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Criteria API: dynaaminen haku
    // Kaikki parametrit ovat valinnaisia
    // GET /api/products/search?minPrice=10&maxPrice=100&categoryId=1
    @GetMapping("/search")
    public List<Product> search(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer categoryId) {
        return productService.searchProducts(minPrice, maxPrice, categoryId);
    }

    // JPQL: massapäivitys - nosta kaikkien kategorian tuotteiden hintaa
    // PUT /api/products/price-update?categoryId=1&multiplier=1.1
    @PutMapping("/price-update")
    @Transactional
    public ResponseEntity<String> updatePrices(
            @RequestParam Integer categoryId,
            @RequestParam BigDecimal multiplier) {
        int updated = productRepository.updatePricesByCategory(multiplier, categoryId);
        return ResponseEntity.ok("Updated " + updated + " products");
    }

    // Osto-operaatio: vähennä varastosaldoa, Optimistinen lukitus estää yliostamisen
    @PutMapping("/{id}/purchase")
    public ResponseEntity<String> purchase(
            @PathVariable Integer id,
            @RequestParam Integer quantity) {
        try {
            return productRepository.findById(id)
                    .map(product -> {
                        if (product.getStockQuantity() < quantity) {
                            return ResponseEntity.badRequest()
                                    .body("Not enough stock. Available: "
                                            + product.getStockQuantity());
                        }
                        // Vähennetään varastoa
                        // Jos toinen transaktio on muuttanut tätä tuotetta
                        // samanaikaisesti → OptimisticLockException
                        product.setStockQuantity(
                                product.getStockQuantity() - quantity);
                        productRepository.save(product);
                        return ResponseEntity.ok(
                                "Purchase successful. New stock: "
                                        + product.getStockQuantity());
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            // OptimisticLockException: toinen transaktio muutti dataa samanaikaisesti
            return ResponseEntity.status(409)
                    .body("Concurrent update detected. Please try again.");
        }
    }

}
