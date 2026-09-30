package fi.metropolia.nurha.webstore.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "orderitems")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    // Yhdistelmäavain: order_id + product_id
    @EmbeddedId
    private OrderItemId id;

    @ManyToOne
    @MapsId("orderId")  // kertoo, että orderId tulee OrderItemId:stä
    @JoinColumn(name = "order_id")
    @JsonBackReference("order-items")
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public OrderItem(Order order, Product product, Integer quantity, BigDecimal unitPrice) {
        this.id = new OrderItemId(order.getId(), product.getId());
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

}
