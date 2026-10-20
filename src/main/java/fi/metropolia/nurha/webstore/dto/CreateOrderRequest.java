package fi.metropolia.nurha.webstore.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {

    private Integer customerId;
    private List<OrderItemRequest> items;
}