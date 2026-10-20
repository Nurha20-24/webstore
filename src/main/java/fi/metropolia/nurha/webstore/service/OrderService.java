package fi.metropolia.nurha.webstore.service;

import fi.metropolia.nurha.webstore.dto.CreateOrderRequest;
import fi.metropolia.nurha.webstore.dto.OrderItemRequest;
import fi.metropolia.nurha.webstore.entity.*;
import fi.metropolia.nurha.webstore.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    // @Transactional takaa että kaikki operaatiot onnistuvat yhdessä
    // tai epäonnistuvat kokonaan — sama kuin SQL:n START TRANSACTION ... COMMIT
    @Transactional
    public Order createOrder(CreateOrderRequest request) {

        // Vaihe 1: tarkista asiakas
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found: " + request.getCustomerId()));

        // Vaihe 2: luo tilaus
        Order order = new Order();
        order.setCustomer(customer);
        Order savedOrder = orderRepository.save(order);

        // Vaihe 3: käy tilausrivit läpi
        for (OrderItemRequest itemRequest : request.getItems()) {

            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found: " + itemRequest.getProductId()));

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException(
                        "Not enough stock for product: " + product.getName()
                                + ". Available: " + product.getStockQuantity()
                                + ", requested: " + itemRequest.getQuantity());
            }

            // Luo tilausrivi
            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    product,
                    itemRequest.getQuantity(),
                    product.getPrice()
            );

            // Vaihe 4: vähennä varasto
            product.setStockQuantity(
                    product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            // Lisää listaan — toimii nyt koska lista on alustettu
            savedOrder.getOrderItems().add(orderItem);
        }

        // Tallennetaan tilaus tilausriveineen — cascade hoitaa orderItemsit
        return orderRepository.save(savedOrder);
    }
}