package com.ecommerce.backend.modules.address.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String fullName;

    private String phone;

    private String province;

    private String district;

    private String ward;

    private String specificAddress;

    private Boolean isDefault;
}
