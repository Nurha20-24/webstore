package fi.metropolia.nurha.webstore.controller;

import fi.metropolia.nurha.webstore.dto.ProductDTO;
import fi.metropolia.nurha.webstore.dto.SupplierDetailDTO;
import fi.metropolia.nurha.webstore.dto.SupplierSummaryDTO;
import fi.metropolia.nurha.webstore.entity.Supplier;
import fi.metropolia.nurha.webstore.repository.SupplierRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepository;

    public SupplierController(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    // GET /api/suppliers — lista, vain perustiedot
    @GetMapping
    public List<SupplierSummaryDTO> getAll() {
        return supplierRepository.findAll()
                .stream()
                .map(SupplierSummaryDTO::new)
                .toList();
    }

    // GET /api/suppliers/{id} — perustiedot + osoite + tuotemäärä
    @GetMapping("/{id}")
    public ResponseEntity<SupplierDetailDTO> getById(@PathVariable Integer id) {
        return supplierRepository.findById(id)
                .map(SupplierDetailDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/suppliers/{id}/products — toimittajan kaikki tuotteet
    // Erillinen päätepiste kun nimenomaan halutaan tuotelista
    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductDTO>> getProducts(@PathVariable Integer id) {
        return supplierRepository.findById(id)
                .map(supplier -> {
                    List<ProductDTO> products = supplier.getProducts()
                            .stream()
                            .map(ProductDTO::new)
                            .toList();
                    return ResponseEntity.ok(products);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/suppliers - luo uusi toimittaja
    @PostMapping
    public SupplierSummaryDTO create(@RequestBody Supplier supplier) {
        return new SupplierSummaryDTO(supplierRepository.save(supplier));
    }

    // PUT /api/suppliers/{id} - päivittää olemassa olevan toimittajan tiedot
    @PutMapping("/{id}")
    public ResponseEntity<SupplierSummaryDTO> update(
            @PathVariable Integer id,
            @RequestBody Supplier updated) {
        return supplierRepository.findById(id)
                .map(supplier -> {
                    supplier.setName(updated.getName());
                    supplier.setContactName(updated.getContactName());
                    supplier.setPhone(updated.getPhone());
                    supplier.setEmail(updated.getEmail());
                    return ResponseEntity.ok(
                            new SupplierSummaryDTO(supplierRepository.save(supplier)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/suppliers/{id} - poistaa toimittajan
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!supplierRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        supplierRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}