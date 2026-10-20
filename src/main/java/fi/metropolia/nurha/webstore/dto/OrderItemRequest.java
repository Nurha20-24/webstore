package fi.metropolia.nurha.webstore.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Kuvaa yhden tilausrivin pyynnössä
// Ei ole entiteetti — vain datan siirtoon
@Getter
@Setter
@NoArgsConstructor
public class OrderItemRequest {

    private Integer productId;
    private Integer quantity;
}