package com.ecommerce.backend.modules.product.enums;

import org.springframework.data.domain.Sort;

public enum ProductSort {
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "basePrice")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "basePrice")),
    RATING_DESC(Sort.by(Sort.Direction.DESC, "averageRating")),
    NAME_ASC(Sort.by(Sort.Direction.ASC, "name"));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
