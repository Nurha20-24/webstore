package fi.metropolia.nurha.webstore.controller;

import fi.metropolia.nurha.webstore.entity.Order;
import fi.metropolia.nurha.webstore.repository.CustomerRepository;
import fi.metropolia.nurha.webstore.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderController(OrderRepository orderRepository,
                           CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public Page<Order> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderRepository.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/with-customers")
    public Page<Order> getAllWithCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderRepository.findAllWithCustomer(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Integer id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST — luo uusi tilaus
    // Testaa muunninta (status pienillä → tallennetaan isoilla)
    // Testaa @PrePersist (orderDate asetetaan automaattisesti)
    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        // Haetaan asiakas id:n perusteella
        if (order.getCustomer() == null || order.getCustomer().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return customerRepository.findById(order.getCustomer().getId())
                .map(customer -> {
                    order.setCustomer(customer);
                    Order saved = orderRepository.save(order);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}