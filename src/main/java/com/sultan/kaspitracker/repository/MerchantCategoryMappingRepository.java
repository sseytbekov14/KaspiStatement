package com.sultan.kaspitracker.repository;

import com.sultan.kaspitracker.entity.MerchantCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantCategoryMappingRepository extends JpaRepository<MerchantCategoryMapping, Long> {
    
    Optional<MerchantCategoryMapping> findByMerchantPattern(String merchantPattern);
}
