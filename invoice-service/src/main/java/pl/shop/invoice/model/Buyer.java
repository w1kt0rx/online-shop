package pl.shop.invoice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Buyer {
    @Column(name = "buyer_first_name", nullable = false)
    private String firstName;

    @Column(name = "buyer_last_name", nullable = false)
    private String lastName;

    @Column(name = "buyer_email", nullable = false)
    private String email;

    public Buyer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
