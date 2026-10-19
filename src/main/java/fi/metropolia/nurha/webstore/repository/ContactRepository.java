package fi.metropolia.nurha.webstore.repository;

import fi.metropolia.nurha.webstore.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

    // Haku sähköpostilla — hyödyllinen tarkistuksissa
    List<Contact> findByEmail(String email);
}