package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
}
