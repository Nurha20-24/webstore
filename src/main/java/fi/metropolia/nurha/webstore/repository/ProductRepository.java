package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // JPQL hakukysely : hae tuotteet hintaluokan mukaan
    // Tämä on massaoperaatio: yksi SELECT hakee kaikki ehdot täyttävät tuotteet
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceRange(
            @Param("minPrice")BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    // JPQL-massapäivitys: nosta hintaa tietyssä kategoriassa
    // @Modifying kertoo että tämä muuttaa dataa (ei pelkkä SELECT)
    // @Transactional vaaditaan kaikille @Modifying-kyselyille, koska ne eivät ole oletuksena transaktion sisällä
    @Modifying
    @Query("UPDATE Product p SET p.price = p.price * :multiplier " + " WHERE p.category.id = :categoryId")
    int updatePricesByCategory(
            @Param("multiplier") BigDecimal multiplier,
            @Param("categoryId") Integer categoryId
    );

    //JPQL-massapoisto: poista tuotteet joiden varasto on 0
    @Modifying
    @Query("DELETE FROM Product p WHERE p.stockQuantity = 0")
    int deleteOutOfStockProducts();


    // Tarkista, onko tuotetta käytetty tilausriveillä
    @Query(value = """
        SELECT COUNT(*)
        FROM orderitems
        WHERE product_id = :id
        """, nativeQuery = true)
    long countOrderItemsByProductId(@Param("id") Integer id);


}

