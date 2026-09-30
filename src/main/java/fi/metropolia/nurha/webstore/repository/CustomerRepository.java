package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
