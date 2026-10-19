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

    // PUT /api/orders/{id} — päivitä tilauksen status ja toimituspäivä
    @PutMapping("/{id}")
    public ResponseEntity<Order> update(
            @PathVariable Integer id,
            @RequestBody Order updated) {
        return orderRepository.findById(id)
                .map(order -> {
                    // Päivitetään vain muutettavat kentät
                    // status muuntuu automaattisesti isoille kirjaimille
                    // OrderStatusConverter:in avulla
                    if (updated.getStatus() != null) {
                        order.setStatus(updated.getStatus());
                    }
                    if (updated.getDeliveryDate() != null) {
                        order.setDeliveryDate(updated.getDeliveryDate());
                    }
                    return ResponseEntity.ok(orderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/orders/{id} — poista tilaus
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!orderRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // CascadeType.ALL poistaa myös tilausrivit automaattisesti
        orderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}