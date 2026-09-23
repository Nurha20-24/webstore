package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {

}

