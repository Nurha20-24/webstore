package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {

}
