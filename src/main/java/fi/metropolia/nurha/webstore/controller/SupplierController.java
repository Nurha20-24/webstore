package fi.metropolia.nurha.webstore.controller;

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

    // Hae kaikki toimittajat osoitteineen ja tuotteineen
    @GetMapping
    public List<Supplier> getAll() {
        return supplierRepository.findAll();
    }

    // Hae yksittäinen toimittaja id:n perusteella
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getById(@PathVariable Integer id) {
        return supplierRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Luo uusi toimittaja
    @PostMapping
    public Supplier create(@RequestBody Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    // Päivitä olemassa oleva toimittaja
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> update(
            @PathVariable Integer id,
            @RequestBody Supplier updated) {
        return supplierRepository.findById(id)
                .map(supplier -> {
                    supplier.setName(updated.getName());
                    supplier.setContactName(updated.getContactName());
                    supplier.setPhone(updated.getPhone());
                    supplier.setEmail(updated.getEmail());
                    return ResponseEntity.ok(supplierRepository.save(supplier));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Poista toimittaja
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!supplierRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        supplierRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
