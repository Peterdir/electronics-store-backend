package com.ecommerce.backend.modules.order.dto.request;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    // Guest sử dụng những trường này
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    // Lấy thông tin Address (Áp dụng cho Registered User)
    private Long addressId;

    @NotBlank(message = "This field is required.")
    private String shippingMethod;

    @NotNull(message = "This field is required.")
    private PaymentMethod paymentMethod;

    private String couponCode;

    @Valid
    private List<CheckoutItem> directItems;

    @Email(message = "Invalid contact email.")
    @Size(max = 254, message = "Contact email is too long.")
    private String contactEmail;
}
