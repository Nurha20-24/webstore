package fi.metropolia.nurha.webstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    // 32 merkin hash-koodi — vahvistus- tai peruutuskoodi
    // char(32) tietokannassa → String Javassa
    @Column(name = "reference", nullable = false, length = 32)
    private String reference;
}