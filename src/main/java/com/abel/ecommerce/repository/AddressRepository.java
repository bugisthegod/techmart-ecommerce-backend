package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Find all addresses by user ID, ordered by default status and creation time
     */
    @Query("SELECT a FROM Address a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<Address> findByUserIdOrderByDefaultAndCreatedAt(@Param("userId") Long userId);

    /**
     * Find user's default address
     */
    Optional<Address> findByUserIdAndIsDefault(Long userId, Integer isDefault);

    /**
     * Check if user has a default address
     */
    boolean existsByUserIdAndIsDefault(Long userId, Integer isDefault);

    /**
     * Count total addresses for a user
     */
    long countByUserId(Long userId);

    /**
     * Clear all default addresses for a user (used when setting new default)
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = 0 WHERE a.userId = :userId")
    void clearDefaultByUserId(@Param("userId") Long userId);

    /**
     * Set specific address as default
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = :isDefault WHERE a.id = :addressId AND a.userId = :userId")
    void updateDefaultStatus(@Param("addressId") Long addressId, @Param("userId") Long userId, @Param("isDefault") Integer isDefault);

    /**
     * Check if address belongs to specific user
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Find address by ID and user ID (security check)
     */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    /**
     * Delete all addresses for a user
     */
    void deleteByUserId(Long userId);
}
