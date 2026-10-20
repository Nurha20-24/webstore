package fi.metropolia.nurha.webstore.dto;

import fi.metropolia.nurha.webstore.entity.Customer;
import lombok.Getter;

@Getter
public class CustomerSummaryDTO {

    private final Integer id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    // ei osoitetta, ei tilauksia

    public CustomerSummaryDTO(Customer customer) {
        this.id = customer.getId();
        this.firstName = customer.getFirst_name();
        this.lastName = customer.getLast_name();
        this.email = customer.getEmail();
        this.phone = customer.getPhone();
    }
}