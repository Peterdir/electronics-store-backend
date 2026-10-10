package com.ecommerce.backend.modules.category.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.category.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private CategoryStatus status;

    private Instant createdAt;

    private Instant updatedAt;

}
