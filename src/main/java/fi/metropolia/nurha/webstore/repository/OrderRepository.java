package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Sivutus toimii automaattisesti JpaRepository:n findAll(Pageable) kautta
    // Spring Data JPA generoi tämän automaattisesti — ei tarvitse @Query:a
    Page<Order> findAll(Pageable pageable);

    // JOIN FETCH asiakkaiden kanssa — ratkaisee N+1 asiakaskyselyissä
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.customer")
    Page<Order> findAllWithCustomer(Pageable pageable);

    // Yksittäinen tilaus tilausriveineen — JOIN FETCH ratkaisee N+1
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Integer id);

    // JPQL-massapäivitys: merkitse vanhat tilaukset EXPIRED-tilaan
    @Modifying
    @Query("UPDATE Order o SET o.status = 'EXPIRED' " +
            "WHERE o.status = 'NEW' " +
            "AND o.orderDate < CURRENT_TIMESTAMP - :days DAY")
    int expireOldOrders(@Param("days") int days);
}