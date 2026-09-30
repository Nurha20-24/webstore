package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
