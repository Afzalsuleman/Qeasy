package com.smartqueue.repository;

import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 * Provides data access methods for user management
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address
     * Used for OTP authentication and user lookup
     *
     * @param email User's email address
     * @return Optional containing User if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists with given email
     * Used to prevent duplicate registrations
     *
     * @param email Email address to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all users with a specific role
     *
     * @param role UserRole to filter by
     * @return List of users with the given role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find user by email and role
     *
     * @param email User's email address
     * @param role UserRole to filter by
     * @return Optional containing User if found
     */
    Optional<User> findByEmailAndRole(String email, UserRole role);

    /**
     * Count users with a specific role
     *
     * @param role UserRole to count
     * @return Number of users with the given role
     */
    long countByRole(UserRole role);
}
