package fi.metropolia.nurha.webstore;

import fi.metropolia.nurha.webstore.entity.ProductCategories;
import fi.metropolia.nurha.webstore.repository.ProductCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebstoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebstoreApplication.class, args);
	}

    @Bean
    CommandLineRunner testRun(ProductCategoryRepository repository) {
        return args -> {
            ProductCategories  category = new ProductCategories ("Testikategoria", "Testiä varten luotu kategoria");
            repository.save(category);

            System.out.println("Kaikki kategoriat:");
            repository.findAll().forEach(c ->
                    System.out.println(c.getId() + ": " + c.getName() + " - " + c.getDescription())
            );
        };
    }

}
