package fi.metropolia.nurha.webstore.dto;

import fi.metropolia.nurha.webstore.entity.Supplier;
import fi.metropolia.nurha.webstore.entity.SupplierAddress;
import lombok.Getter;

@Getter
public class SupplierDetailDTO {

    private final Integer id;
    private final String name;
    private final String contactName;
    private final String phone;
    private final String email;

    // Osoite — vain ensimmäinen osoite (useimmilla toimittajilla yksi)
    private final String streetAddress;
    private final String postalCode;
    private final String city;
    private final String country;

    // Tuotteiden määrä — ei koko tuotelistaa
    private final int productCount;

    public SupplierDetailDTO(Supplier supplier) {
        this.id = supplier.getId();
        this.name = supplier.getName();
        this.contactName = supplier.getContactName();
        this.phone = supplier.getPhone();
        this.email = supplier.getEmail();

        // Otetaan ensimmäinen osoite jos on
        if (supplier.getAddresses() != null && !supplier.getAddresses().isEmpty()) {
            SupplierAddress address = supplier.getAddresses().get(0);
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

        this.productCount = supplier.getProducts() != null
                ? supplier.getProducts().size()
                : 0;
    }
}