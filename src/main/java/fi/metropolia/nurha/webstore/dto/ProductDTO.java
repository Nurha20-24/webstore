// src/main/java/fi/metropolia/nurha/webstore/dto/ProductDTO.java

package fi.metropolia.nurha.webstore.dto;

import fi.metropolia.nurha.webstore.entity.Product;
import lombok.Getter;

import java.math.BigDecimal;

// DTO = Data Transfer Object
// Määrittelee tarkalleen mitä dataa palautetaan API:sta
// Ei ole JPA-entiteetti — ei @Entity-annotaatiota
@Getter
public class ProductDTO {

    private final Integer id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer stockQuantity;
    private final Long version;

    // Kategorian tiedot — vain id ja nimi, ei koko objektia
    private final Integer categoryId;
    private final String categoryName;

    // Konstruktori ottaa Product-entiteetin ja poimii siitä tarvittavat kentät
    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.version = product.getVersion();

        // Kategoria voi olla null jos tuotteella ei ole kategoriaa
        if (product.getCategory() != null) {
            this.categoryId = product.getCategory().getId();
            this.categoryName = product.getCategory().getName();
        } else {
            this.categoryId = null;
            this.categoryName = null;
        }
    }
}