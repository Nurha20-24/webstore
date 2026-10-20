package fi.metropolia.nurha.webstore.controller;

import fi.metropolia.nurha.webstore.dto.CustomerDetailDTO;
import fi.metropolia.nurha.webstore.dto.CustomerSummaryDTO;
import fi.metropolia.nurha.webstore.entity.CompanyCustomer;
import fi.metropolia.nurha.webstore.entity.Customer;
import fi.metropolia.nurha.webstore.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // GET /api/customers — lista, vain perustiedot
    @GetMapping
    public List<CustomerSummaryDTO> getAll() {
        return customerRepository.findAll()
                .stream()
                .map(CustomerSummaryDTO::new)
                .toList();
    }

    // GET /api/customers/{id} — yksi asiakas, perustiedot + osoite + tilauksien määrä
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailDTO> getById(@PathVariable Integer id) {
        return customerRepository.findById(id)
                .map(CustomerDetailDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/customers - luo uusi asiakas
    @PostMapping
    public CustomerSummaryDTO create(@RequestBody Customer customer) {
        return new CustomerSummaryDTO(customerRepository.save(customer));
    }

    // POST /api/customers/company - luo uusi yritysasiakas
    @PostMapping("/company")
    public CustomerSummaryDTO createCompany(@RequestBody CompanyCustomer customer) {
        return new CustomerSummaryDTO(customerRepository.save(customer));
    }

    // PUT /api/customers/{id} - päivittää asiakkaan tiedot
    @PutMapping("/{id}")
    public ResponseEntity<CustomerSummaryDTO> update(
            @PathVariable Integer id,
            @RequestBody Customer updated) {
        return customerRepository.findById(id)
                .map(customer -> {
                    customer.setFirst_name(updated.getFirst_name());
                    customer.setLast_name(updated.getLast_name());
                    customer.setEmail(updated.getEmail());
                    customer.setPhone(updated.getPhone());
                    return ResponseEntity.ok(
                            new CustomerSummaryDTO(customerRepository.save(customer)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/customers/{id} - poistaa asiakkaan
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!customerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        customerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}