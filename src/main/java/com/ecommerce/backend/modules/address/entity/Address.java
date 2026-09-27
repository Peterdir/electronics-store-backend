package com.ecommerce.backend.modules.address.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {

    @Id
    @Tsid
    private Long id;

    private String fullName;

    private String phone;

    private String province;

    private String district;

    private String ward;

    private String specificAddress;

    private Boolean isDefault;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
