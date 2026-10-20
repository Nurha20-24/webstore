package fi.metropolia.nurha.webstore.dto;

import fi.metropolia.nurha.webstore.entity.Customer;
import fi.metropolia.nurha.webstore.entity.CustomerAddress;
import lombok.Getter;

@Getter
public class CustomerDetailDTO {

    private final Integer id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;

    // Osoite — vain tarvittavat kentät
    private final String streetAddress;
    private final String postalCode;
    private final String city;
    private final String country;

    // Tilausten määrä — ei koko tilausobjekteja
    private final int orderCount;

    public CustomerDetailDTO(Customer customer) {
        this.id = customer.getId();
        this.firstName = customer.getFirst_name();
        this.lastName = customer.getLast_name();
        this.email = customer.getEmail();
        this.phone = customer.getPhone();

        CustomerAddress address = customer.getAddress();
        if (address != null) {
            this.streetAddress = address.getStreetAddress();
            this.postalCode = address.getPostalCode();
            this.city = address.getCity();
            this.country = address.getCountry();
        } else {
            this.streetAddress = null;
            this.postalCode = null;
            this.city = null;
            this.country = null;
        }

        // Tilausten määrä listana — ei koko tilausobjekteja
        this.orderCount = customer.getOrders() != null
                ? customer.getOrders().size()
                : 0;
    }
}