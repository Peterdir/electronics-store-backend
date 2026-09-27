package com.ecommerce.backend.modules.address.repository;

import com.ecommerce.backend.modules.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    Boolean existsByUserId(Long userId);
}
