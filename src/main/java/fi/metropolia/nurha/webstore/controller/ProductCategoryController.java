package fi.metropolia.nurha.webstore.controller;

import fi.metropolia.nurha.webstore.entity.ProductCategory;
import fi.metropolia.nurha.webstore.repository.ProductCategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class ProductCategoryController {

    private final ProductCategoryRepository repository;

    public ProductCategoryController(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductCategory> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategory> getById(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ProductCategory create(@RequestBody ProductCategory category) {
        return repository.save(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategory> update(
            @PathVariable Integer id,
            @RequestBody ProductCategory updated) {
        return repository.findById(id)
                .map(category -> {
                category.setName(updated.getName());
                category.setDescription(updated.getDescription());
                return ResponseEntity.ok(repository.save(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
