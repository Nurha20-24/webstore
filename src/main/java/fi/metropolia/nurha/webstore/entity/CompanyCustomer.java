package fi.metropolia.nurha.webstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("COMPANY")      // tämä arvo tallennetaan kun kyseessä on yritysasiakas
@Getter
@Setter
@NoArgsConstructor
public class CompanyCustomer extends Customer {

    @Column(name = "company_name",  length = 255)
    private String companyName;

    @Column(name = "business_id", length = 20)
    private String businessId;

    public CompanyCustomer(String firstName, String lastName, String email, String companyName, String businessId) {
        super();
        setFirst_name(firstName);
        setLast_name(lastName);
        setEmail(email);
        this.companyName = companyName;
        this.businessId = businessId;
    }

}
