package com.smartqueue.repository;

import com.smartqueue.model.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Shop entity
 * Provides data access methods for shop management
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    /**
     * Find shop by owner ID
     * MVP restriction: One active shop per owner
     *
     * @param ownerId Owner's user ID
     * @return Optional containing Shop if found
     */
    Optional<Shop> findByOwnerId(UUID ownerId);

    /**
     * Find active shop by owner ID
     * Used to enforce one-active-shop-per-owner rule
     *
     * @param ownerId Owner's user ID
     * @param isActive Shop active status (should be true)
     * @return Optional containing Shop if found
     */
    Optional<Shop> findByOwnerIdAndIsActive(UUID ownerId, Boolean isActive);

    /**
     * Find all active shops
     * Used for scheduler (no-show detection) and admin queries
     *
     * @return List of active shops
     */
    List<Shop> findByIsActiveTrue();

    /**
     * Check if user already owns an active shop
     * Used in shop creation validation
     *
     * @param ownerId Owner's user ID
     * @return true if user owns an active shop
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Shop s WHERE s.owner.id = :ownerId AND s.isActive = true")
    boolean existsByOwnerIdAndIsActiveTrue(UUID ownerId);

    /**
     * Get all shop IDs (for scheduler optimization)
     * Returns only IDs to reduce memory usage
     *
     * @return List of shop IDs
     */
    @Query("SELECT s.id FROM Shop s WHERE s.isActive = true")
    List<UUID> findAllActiveShopIds();
}
