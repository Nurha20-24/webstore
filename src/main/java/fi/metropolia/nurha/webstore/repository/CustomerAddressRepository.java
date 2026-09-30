package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Integer> {
}
