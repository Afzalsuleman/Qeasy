package com.smartqueue.service;

import com.smartqueue.exception.ShopAlreadyExistsException;
import com.smartqueue.exception.ShopNotFoundException;
import com.smartqueue.exception.UnauthorizedException;
import com.smartqueue.model.dto.CreateShopRequest;
import com.smartqueue.model.dto.ShopResponse;
import com.smartqueue.model.dto.UpdateShopRequest;
import com.smartqueue.model.entity.Shop;
import com.smartqueue.model.entity.User;
import com.smartqueue.repository.ShopRepository;
import com.smartqueue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shop service for CRUD operations
 * Handles shop creation, updates, and configuration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_QUEUE_KEY_PREFIX = "queue:";

    /**
     * Create a new shop for the authenticated user
     *
     * @param request CreateShopRequest with shop details
     * @param ownerEmail Owner's email from JWT
     * @return ShopResponse with created shop details
     * @throws ShopAlreadyExistsException if owner already has an active shop
     */
    @Transactional
    public ShopResponse createShop(CreateShopRequest request, String ownerEmail) {
        log.info("Creating shop for owner: {}", ownerEmail);

        // Get owner user
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Check if owner already has an active shop (business rule: one shop per owner)
        if (shopRepository.existsByOwnerIdAndIsActiveTrue(owner.getId())) {
            throw new ShopAlreadyExistsException("You already have an active shop. Deactivate it before creating a new one.");
        }

        // Create shop entity
        Shop shop = Shop.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .avgServiceTimeMinutes(request.getAvgServiceTimeMinutes())
                .maxQueueSize(request.getMaxQueueSize())
                .isActive(true)
                .build();

        shop = shopRepository.save(shop);

        log.info("Shop created successfully: {} (ID: {})", shop.getName(), shop.getId());

        return mapToResponse(shop, 0);
    }

    /**
     * Get shop by ID
     *
     * @param shopId Shop UUID
     * @return ShopResponse with shop details and current queue size
     */
    @Transactional(readOnly = true)
    public ShopResponse getShopById(UUID shopId) {
        log.info("Getting shop by ID: {}", shopId);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Get current queue size from Redis
        int currentQueueSize = getCurrentQueueSize(shopId);

        return mapToResponse(shop, currentQueueSize);
    }

    /**
     * Get all shops (paginated in future)
     *
     * @return List of ShopResponse
     */
    @Transactional(readOnly = true)
    public List<ShopResponse> getAllShops() {
        log.info("Getting all active shops");

        return shopRepository.findByIsActiveTrue().stream()
                .map(shop -> {
                    int queueSize = getCurrentQueueSize(shop.getId());
                    return mapToResponse(shop, queueSize);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get shop by owner email
     *
     * @param ownerEmail Owner's email
     * @return ShopResponse with shop details
     */
    @Transactional(readOnly = true)
    public ShopResponse getShopByOwner(String ownerEmail) {
        log.info("Getting shop for owner: {}", ownerEmail);

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        Shop shop = shopRepository.findByOwnerIdAndIsActive(owner.getId(), true)
                .orElseThrow(() -> new ShopNotFoundException("No active shop found for owner"));

        int currentQueueSize = getCurrentQueueSize(shop.getId());

        return mapToResponse(shop, currentQueueSize);
    }

    /**
     * Update shop configuration
     *
     * @param shopId Shop UUID
     * @param request UpdateShopRequest with updated fields
     * @param ownerEmail Owner's email from JWT (for authorization)
     * @return ShopResponse with updated shop details
     */
    @Transactional
    public ShopResponse updateShop(UUID shopId, UpdateShopRequest request, String ownerEmail) {
        log.info("Updating shop: {} by owner: {}", shopId, ownerEmail);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Verify ownership
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!shop.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedException("You are not authorized to update this shop");
        }

        // Update fields if provided
        if (request.getName() != null) {
            shop.setName(request.getName());
        }
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            shop.setAddress(request.getAddress());
        }
        if (request.getAvgServiceTimeMinutes() != null) {
            shop.setAvgServiceTimeMinutes(request.getAvgServiceTimeMinutes());
        }
        if (request.getMaxQueueSize() != null) {
            shop.setMaxQueueSize(request.getMaxQueueSize());
        }
        if (request.getIsActive() != null) {
            shop.setIsActive(request.getIsActive());
        }

        shop = shopRepository.save(shop);

        log.info("Shop updated successfully: {}", shop.getId());

        int currentQueueSize = getCurrentQueueSize(shopId);
        return mapToResponse(shop, currentQueueSize);
    }

    /**
     * Delete shop (soft delete by setting isActive = false)
     *
     * @param shopId Shop UUID
     * @param ownerEmail Owner's email from JWT (for authorization)
     */
    @Transactional
    public void deleteShop(UUID shopId, String ownerEmail) {
        log.info("Deleting shop: {} by owner: {}", shopId, ownerEmail);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Verify ownership
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!shop.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this shop");
        }

        // Soft delete
        shop.setIsActive(false);
        shopRepository.save(shop);

        log.info("Shop deactivated successfully: {}", shop.getId());
    }

    /**
     * Get current queue size from Redis
     */
    private int getCurrentQueueSize(UUID shopId) {
        try {
            String queueKey = REDIS_QUEUE_KEY_PREFIX + shopId;
            Long size = redisTemplate.opsForZSet().zCard(queueKey);
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            log.warn("Failed to get queue size from Redis for shop {}: {}", shopId, e.getMessage());
            return 0;
        }
    }

    /**
     * Map Shop entity to ShopResponse DTO
     */
    private ShopResponse mapToResponse(Shop shop, int currentQueueSize) {
        int estimatedWaitTime = currentQueueSize * shop.getAvgServiceTimeMinutes();

        return ShopResponse.builder()
                .id(shop.getId())
                .ownerId(shop.getOwner().getId())
                .name(shop.getName())
                .description(shop.getDescription())
                .address(shop.getAddress())
                .phone(null)
                .avgServiceTimeMinutes(shop.getAvgServiceTimeMinutes())
                .maxQueueSize(shop.getMaxQueueSize())
                .isActive(shop.getIsActive())
                .currentQueueSize(currentQueueSize)
                .estimatedWaitTimeMinutes(estimatedWaitTime)
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}
