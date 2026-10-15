package fi.metropolia.nurha.webstore.service;

import fi.metropolia.nurha.webstore.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    @PersistenceContext
    private EntityManager entityManager;

    // Criteria API: dynaaminen haku usealla valinnaisella ehdolla
    public List<Product> searchProducts(BigDecimal minPrice, BigDecimal maxPrice, Integer categoryId) {

        // Luodaan CriteriaBuilder-olio, joka on Criteria API:n pääkomponentti
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Luodaan kysely joka palautta Product-oliota
        CriteriaQuery<Product> query = cb.createQuery(Product.class);

        // määritellään FROM-osa — "mistä taulusta haetaan"
        Root<Product> product = query.from(Product.class);

        // Luodaan dynaaminen WHERE-osa
        //  Lisätään ehto vain jos parametri on annettu
        List<Predicate> predicates = new ArrayList<>();

        if (minPrice != null) {
            // p.price >= minPrice
            predicates.add(cb.greaterThanOrEqualTo(product.get("price"), minPrice));
        }
        if (maxPrice != null) {
            // p.price <= maxPrice
            predicates.add(cb.lessThanOrEqualTo(product.get("price"), maxPrice));
        }
        if (categoryId != null) {
            // p.category.id = categoryId
            predicates.add(cb.equal(product.get("category").get("id"), categoryId));
        }

        // yhdistetään ehdot AND-operaattorilla
        query.select(product).where(predicates.toArray(new  Predicate[0]));

        // suoritetaan kysely ja palautetaan tulokset
        return entityManager.createQuery(query).getResultList();
    }

    // @Transactional vaaditaan kun tehdään muutoksia (@Modifying-kyselyt)
    // Ilman tätä Spring heittää TransactionRequiredException
    @Transactional
    public int updatePricesByCategory(Integer categoryId, BigDecimal multiplier) {
        // Tässä kutsutaan repositoryn JPQL-kyselyä
        // Palautetaan muutettujen rivien määrä
        return entityManager.createQuery("UPDATE Product p SET p.price = p.price * :multiplier " +
                "WHERE p.category.id = :categoryId")
                .setParameter("multiplier", multiplier)
                .setParameter("categoryId", categoryId)
                .executeUpdate();
    }
}
