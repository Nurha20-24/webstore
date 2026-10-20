package fi.metropolia.nurha.webstore.controller;


import fi.metropolia.nurha.webstore.dto.ProductDTO;
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

    public ProductController(ProductRepository productRepository,
                             ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    // GET /api/products — palauttaa DTO:n jossa näkyy kategoria
    @GetMapping
    public List<ProductDTO> getAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductDTO::new)  // muunnetaan jokainen Product → ProductDTO
                .toList();
    }

    // GET /api/products/{id} — palauttaa DTO:n
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Integer id) {
        return productRepository.findById(id)
                .map(ProductDTO::new)  // Product → ProductDTO
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/products — pyyntö tulee entiteettinä, vastaus DTO:na
    @PostMapping
    public ProductDTO create(@RequestBody Product product) {
        return new ProductDTO(productRepository.save(product));
    }

    // PUT /api/products/{id} - päivittää tuotteen, jos löytyy, palauttaa DTO:n
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(
            @PathVariable Integer id,
            @RequestBody Product updated) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(updated.getName());
                    product.setDescription(updated.getDescription());
                    product.setPrice(updated.getPrice());
                    product.setStockQuantity(updated.getStockQuantity());
                    return ResponseEntity.ok(
                            new ProductDTO(productRepository.save(product)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/products/{id} - poistaa tuotteen, jos löytyy
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/products/search — Criteria API, palauttaa DTO:n
    @GetMapping("/search")
    public List<ProductDTO> search(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer categoryId) {
        return productService.searchProducts(minPrice, maxPrice, categoryId)
                .stream()
                .map(ProductDTO::new)
                .toList();
    }

    // PUT /api/products/price-update - päivittää hintaa kertoimella kaikille tuotteille tietyssä kategoriassa
    @PutMapping("/price-update")
    @Transactional
    public ResponseEntity<String> updatePrices(
            @RequestParam Integer categoryId,
            @RequestParam BigDecimal multiplier) {
        int updated = productRepository.updatePricesByCategory(
                multiplier, categoryId);
        return ResponseEntity.ok("Updated " + updated + " products");
    }

    // PUT /api/products/{id}/purchase - vähentää varastomäärää, tarkistaa että varastoa on tarpeeksi
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
                        product.setStockQuantity(
                                product.getStockQuantity() - quantity);
                        productRepository.save(product);
                        return ResponseEntity.ok(
                                "Purchase successful. New stock: "
                                        + product.getStockQuantity());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(409)
                    .body("Concurrent update detected. Please try again.");
        }
    }
}