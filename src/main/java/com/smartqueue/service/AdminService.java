package com.smartqueue.service;

import com.smartqueue.exception.ShopNotFoundException;
import com.smartqueue.exception.UnauthorizedException;
import com.smartqueue.exception.UserAlreadyExistsException;
import com.smartqueue.model.dto.CreateShopOwnerRequest;
import com.smartqueue.model.dto.UserResponse;
import com.smartqueue.model.entity.Shop;
import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.UserRole;
import com.smartqueue.repository.ShopRepository;
import com.smartqueue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin service for managing shop owners and system analytics
 * Only accessible by users with ADMIN role
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new shop owner account and send invitation email
     *
     * @param request CreateShopOwnerRequest with shop owner details
     * @param adminEmail Admin's email from JWT (for audit)
     * @return UserResponse with created shop owner details
     * @throws UserAlreadyExistsException if user already exists
     * @throws UnauthorizedException if user is not admin
     */
    @Transactional
    public UserResponse createShopOwner(CreateShopOwnerRequest request, String adminEmail) {
        log.info("Admin {} creating shop owner with email: {}", adminEmail, request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Generate temporary password
        String temporaryPassword = generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        // Create shop owner user
        User shopOwner = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .role(UserRole.SHOP_OWNER)
                .password(encodedPassword)
                .passwordSet(true)
                .build();

        shopOwner = userRepository.save(shopOwner);

        log.info("Shop owner created successfully: {} (ID: {}) with temporary password", shopOwner.getEmail(), shopOwner.getId());

        // Send invitation email with temporary credentials
        emailService.sendShopOwnerInvitationWithPassword(shopOwner, temporaryPassword);

        return mapToResponse(shopOwner);
    }

    /**
     * Get all shop owners
     *
     * @return List of shop owners
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllShopOwners() {
        log.info("Fetching all shop owners");

        return userRepository.findByRole(UserRole.SHOP_OWNER).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get shop owner by ID
     *
     * @param shopOwnerId Shop owner's user ID
     * @return UserResponse with shop owner details
     */
    @Transactional(readOnly = true)
    public UserResponse getShopOwner(String shopOwnerId) {
        log.info("Fetching shop owner: {}", shopOwnerId);

        User shopOwner = userRepository.findByEmailAndRole(shopOwnerId, UserRole.SHOP_OWNER)
                .orElseThrow(() -> new UnauthorizedException("Shop owner not found"));

        return mapToResponse(shopOwner);
    }

    /**
     * Get system dashboard data for admin
     * Includes total shops, active shops, and other system metrics
     *
     * @param adminEmail Admin's email from JWT
     * @return Dashboard data with shop count and aggregated metrics
     */
    @Transactional(readOnly = true)
    public AdminDashboardData getDashboardData(String adminEmail) {
        log.info("Fetching dashboard data for admin: {}", adminEmail);

        long totalShops = shopRepository.count();
        long activeShops = shopRepository.countByIsActiveTrue();
        long totalShopOwners = userRepository.countByRole(UserRole.SHOP_OWNER);
        long totalUsers = userRepository.count();

        return AdminDashboardData.builder()
                .totalShops(totalShops)
                .activeShops(activeShops)
                .inactiveShops(totalShops - activeShops)
                .totalShopOwners(totalShopOwners)
                .totalUsers(totalUsers)
                .build();
    }

    /**
     * Get all shops with their details (for admin dashboard)
     *
     * @param adminEmail Admin's email from JWT
     * @return List of all shops with current queue data
     */
    @Transactional(readOnly = true)
    public List<Shop> getAllShopsWithDetails(String adminEmail) {
        log.info("Fetching all shops for admin: {}", adminEmail);

        return shopRepository.findAll();
    }

    /**
     * Get analytics for a specific shop (admin can view all)
     *
     * @param shopId Shop UUID
     * @param adminEmail Admin's email from JWT
     * @return Shop with analytics data
     */
    @Transactional(readOnly = true)
    public Shop getShopAnalytics(String shopId, String adminEmail) {
        log.info("Admin {} fetching analytics for shop: {}", adminEmail, shopId);

        Shop shop = shopRepository.findById(java.util.UUID.fromString(shopId))
                .orElseThrow(() -> new ShopNotFoundException("Shop not found"));

        return shop;
    }

    /**
     * Generate a secure temporary password for shop owner
     * Format: 8 characters with mix of uppercase, lowercase, numbers, and special characters
     *
     * @return Temporary password string
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole())
                .passwordSet(user.getPasswordSet())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Admin Dashboard Data DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class AdminDashboardData {
        private long totalShops;
        private long activeShops;
        private long inactiveShops;
        private long totalShopOwners;
        private long totalUsers;
    }
}
