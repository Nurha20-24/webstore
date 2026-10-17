package fi.metropolia.nurha.webstore.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import fi.metropolia.nurha.webstore.converter.OrderStatusConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name="orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;


    @Column(name = "status")
    @Convert(converter = OrderStatusConverter.class)
    private String status;


    // M:1 - monta tilausta kuuluu yhdelle asiakkaalle
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    @JsonBackReference("customer-orders")
    private Customer customer;

    // 1:M - yhdellä tilauksella voi olla useita tilausrivejä (OrderItem)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonManagedReference("order-items")
    private List<OrderItem> orderItems;

    // JPA kutsuu tätä automaattisesti ENNEN kuin tilaus tallennetaan
    // Varmistaa, että orderDate on asetettu ja status on oletuksena "NEW"
    @PrePersist
    public void setOrderDateOnCreate() {
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = "NEW";
        } else {
            this.status = this.status.trim().toUpperCase(Locale.ROOT);
        }
    }
}