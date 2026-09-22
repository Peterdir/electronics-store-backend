package com.ecommerce.backend.modules.auth.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;


}
