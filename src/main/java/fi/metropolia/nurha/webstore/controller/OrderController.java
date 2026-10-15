package fi.metropolia.nurha.webstore.controller;

import fi.metropolia.nurha.webstore.entity.Order;
import fi.metropolia.nurha.webstore.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }


    //sivutus + JOIN FETCH → hallittu määrä dataa
    @GetMapping
    public Page<Order> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderRepository.findAll(PageRequest.of(page, size));
    }

    // Vaihtoehto: kaikki tilaukset JOIN FETCHillä ilman sivutusta
    @GetMapping("/with-customers")
    public Page<Order> getAllWithCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderRepository.findAllWithCustomer(PageRequest.of(page, size));
    }

    // Hae yksi tilaus tilausriveineen
    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Integer id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}