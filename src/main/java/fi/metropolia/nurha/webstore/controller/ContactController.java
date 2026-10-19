package fi.metropolia.nurha.webstore.controller;

import fi.metropolia.nurha.webstore.entity.Contact;
import fi.metropolia.nurha.webstore.repository.ContactRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactRepository contactRepository;

    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    // Hae kaikki kontaktit
    @GetMapping
    public List<Contact> getAll() {
        return contactRepository.findAll();
    }

    // Hae kontakti ID:llä
    @GetMapping("/{id}")
    public ResponseEntity<Contact> getById(@PathVariable Integer id) {
        return contactRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Hae kontakti sähköpostilla
    @GetMapping("/email/{email}")
    public ResponseEntity<List<Contact>> getByEmail(@PathVariable String email) {
        List<Contact> contacts = contactRepository.findByEmail(email);

        if (contacts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(contacts);
    }

    // Luo uusi kontakti
    // reference generoidaan automaattisesti — ei tarvita pyynnössä
    @PostMapping
    public Contact create(@RequestBody Contact contact) {
        // Generoidaan 32 merkin MD5-muotoinen reference automaattisesti
        // UUID ilman väliviivoja = 32 merkkiä
        if (contact.getReference() == null || contact.getReference().isBlank()) {
            contact.setReference(
                    UUID.randomUUID().toString().replace("-", "")
            );
        }
        return contactRepository.save(contact);
    }

    // Poista kontakti
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!contactRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        contactRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}