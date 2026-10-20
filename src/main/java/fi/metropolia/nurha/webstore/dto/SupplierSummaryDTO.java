package fi.metropolia.nurha.webstore.dto;

import fi.metropolia.nurha.webstore.entity.Supplier;
import lombok.Getter;

@Getter
public class SupplierSummaryDTO {

    private final Integer id;
    private final String name;
    private final String contactName;
    private final String phone;
    private final String email;

    public SupplierSummaryDTO(Supplier supplier) {
        this.id = supplier.getId();
        this.name = supplier.getName();
        this.contactName = supplier.getContactName();
        this.phone = supplier.getPhone();
        this.email = supplier.getEmail();
    }
}