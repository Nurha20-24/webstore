package fi.metropolia.nurha.webstore.controller;

import fi.metropolia.nurha.webstore.dto.CreateOrderRequest;
import fi.metropolia.nurha.webstore.entity.Order;
import fi.metropolia.nurha.webstore.repository.CustomerRepository;
import fi.metropolia.nurha.webstore.repository.OrderRepository;
import fi.metropolia.nurha.webstore.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;

    public OrderController(OrderRepository orderRepository,
                           CustomerRepository customerRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderService = orderService;
    }

    // GET /api/orders — lista, vain perustiedot
    @GetMapping
    public Page<Order> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderRepository.findAll(PageRequest.of(page, size));
    }

    // GET /api/orders/with-customers — lista, mukana asiakastiedot
    @GetMapping("/with-customers")
    public Page<Order> getAllWithCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderRepository.findAllWithCustomer(PageRequest.of(page, size));
    }

    // GET /api/orders/{id} - haetaan yksi tilaus id:n perusteella
    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Integer id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST — luo uusi tilaus
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

    // Luo tilauksen, tilausrivit ja päivittää varastosaldot atomisesti
    @PostMapping("/complete")
    public ResponseEntity<?> createComplete(
            @RequestBody CreateOrderRequest request) {
        try {
            Order order = orderService.createOrder(request);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            // Transaktio on jo peruutettu automaattisesti
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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