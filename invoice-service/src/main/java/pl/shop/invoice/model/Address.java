package pl.shop.invoice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {
    @Column(name = "billing_street", nullable = false)
    private String street;

    @Column(name = "billing_city", nullable = false)
    private String city;

    @Column(name = "billing_postal_code", nullable = false)
    private String postalCode;

    @Column(name = "billing_country", nullable = false)
    private String country;

    public Address(String street, String city, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }
}
