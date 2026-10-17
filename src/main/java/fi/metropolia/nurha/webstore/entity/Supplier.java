package fi.metropolia.nurha.webstore.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "Suppliers")
@Getter
@Setter
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    // 1:M — toimittajalla monta osoitetta
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @JsonManagedReference("supplier-addresses")
    private List<SupplierAddress> addresses;

    // 1:M — toimittajalla monta tuotetta
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    @JsonManagedReference("supplier-products")
    private List<Product> products;
}