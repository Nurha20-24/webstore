package fi.metropolia.nurha.webstore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "productcategories ")
public class ProductCategories  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // JPA requires a no-argument constructor
    public ProductCategories () {
    }

    public ProductCategories (String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
