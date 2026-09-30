package fi.metropolia.nurha.webstore.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name="Customers")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "customer_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("PERSONAL")     // tämä arvo tallennetaan kun kyseessä on Customer
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String first_name;

    @Column(name = "last_name", nullable = false, length = 100 )
    private String last_name;

    @Column(name = "email", nullable = false, unique = true, length = 250)
    private String email;

    @Column(name = "phone", length = 130)
    private String phone;

    // 1:1 - asiakkaalla on yksi toimitusosoite
    // mappedBy = "customer" -  CustomerAddress omistaa suhteen, siellä on vierasavain
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference("customer-address")
    private CustomerAddress address;

    // 1:M - yhdellä asiakkaalla voi olla useita tilauksia
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @JsonManagedReference("customer-orders")
    private List<Order> orders;

}
