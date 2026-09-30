package fi.metropolia.nurha.webstore.controller;

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

    @GetMapping
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(@PathVariable Integer id) {
        return customerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Luo tavallinen henkilöasiakas
    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @PostMapping("/company")
    public CompanyCustomer createCompany(@RequestBody CompanyCustomer customer) {
        return (CompanyCustomer) customerRepository.save(customer);
    }
}
