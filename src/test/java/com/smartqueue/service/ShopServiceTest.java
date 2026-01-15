package com.smartqueue.service;

import com.smartqueue.exception.ShopAlreadyExistsException;
import com.smartqueue.exception.ShopNotFoundException;
import com.smartqueue.exception.UnauthorizedException;
import com.smartqueue.model.dto.CreateShopRequest;
import com.smartqueue.model.dto.ShopResponse;
import com.smartqueue.model.dto.UpdateShopRequest;
import com.smartqueue.model.entity.Shop;
import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.UserRole;
import com.smartqueue.repository.ShopRepository;
import com.smartqueue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShopService
 * Tests CRUD operations and business logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService Tests")
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private ShopService shopService;

    private User testOwner;
    private Shop testShop;
    private CreateShopRequest createShopRequest;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        testOwner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .name("Shop Owner")
                .role(UserRole.SHOP_OWNER)
                .build();

        testShop = Shop.builder()
                .id(UUID.randomUUID())
                .owner(testOwner)
                .name("Test Shop")
                .description("Test Description")
                .address("123 Main St")
                .maxQueueSize(50)
                .avgServiceTimeMinutes(15)
                .isActive(true)
                .build();

        createShopRequest = CreateShopRequest.builder()
                .name("Test Shop")
                .description("Test Description")
                .address("123 Main St")
                .maxQueueSize(50)
                .avgServiceTimeMinutes(15)
                .build();
    }

    @Test
    @DisplayName("Should create shop with valid data")
    void shouldCreateShopWithValidData() {
        // Given
        CreateShopRequest validRequest = CreateShopRequest.builder()
                .name("New Shop")
                .description("Description")
                .address("123 Street")
                .maxQueueSize(50)
                .avgServiceTimeMinutes(15)
                .build();

        Shop savedShop = Shop.builder()
                .id(UUID.randomUUID())
                .owner(testOwner)
                .name("New Shop")
                .maxQueueSize(50)
                .avgServiceTimeMinutes(15)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testOwner));
        when(shopRepository.save(any(Shop.class))).thenReturn(savedShop);

        // When
        ShopResponse response = shopService.createShop(validRequest, "owner@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("New Shop");
        verify(shopRepository).save(any(Shop.class));
    }

    @Test
    @DisplayName("Should get shop by ID successfully")
    void shouldGetShopById() {
        // Given
        when(shopRepository.findById(testShop.getId())).thenReturn(Optional.of(testShop));
        when(zSetOperations.zCard(any())).thenReturn(3L);

        // When
        ShopResponse response = shopService.getShopById(testShop.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testShop.getId());
        assertThat(response.getName()).isEqualTo("Test Shop");
        assertThat(response.getCurrentQueueSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should throw exception when shop not found")
    void shouldThrowExceptionWhenShopNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(shopRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> shopService.getShopById(nonExistentId))
                .isInstanceOf(ShopNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should get shop by owner email")
    void shouldGetShopByOwnerEmail() {
        // Given
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testOwner));
        when(shopRepository.findByOwnerIdAndIsActive(testOwner.getId(), true)).thenReturn(Optional.of(testShop));
        when(zSetOperations.zCard(any())).thenReturn(0L);

        // When
        ShopResponse response = shopService.getShopByOwner("owner@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Shop");
    }

    @Test
    @DisplayName("Should update shop successfully")
    void shouldUpdateShopSuccessfully() {
        // Given
        UpdateShopRequest updateRequest = UpdateShopRequest.builder()
                .name("Updated Shop")
                .description("Updated Description")
                .maxQueueSize(100)
                .build();

        Shop updatedShop = testShop;
        updatedShop.setName("Updated Shop");
        updatedShop.setDescription("Updated Description");
        updatedShop.setMaxQueueSize(100);

        when(shopRepository.findById(testShop.getId())).thenReturn(Optional.of(testShop));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testOwner));
        when(shopRepository.save(any(Shop.class))).thenReturn(updatedShop);
        when(zSetOperations.zCard(any())).thenReturn(0L);

        // When
        ShopResponse response = shopService.updateShop(testShop.getId(), updateRequest, "owner@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Shop");
        assertThat(response.getMaxQueueSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should throw exception when unauthorized user tries to update shop")
    void shouldThrowExceptionWhenUnauthorized() {
        // Given
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .name("Other User")
                .build();

        UpdateShopRequest updateRequest = UpdateShopRequest.builder()
                .name("Hacked Shop")
                .build();

        when(shopRepository.findById(testShop.getId())).thenReturn(Optional.of(testShop));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        // When/Then
        assertThatThrownBy(() -> shopService.updateShop(testShop.getId(), updateRequest, "other@example.com"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("Should delete shop successfully")
    void shouldDeleteShopSuccessfully() {
        // Given
        when(shopRepository.findById(testShop.getId())).thenReturn(Optional.of(testShop));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testOwner));
        when(shopRepository.save(any(Shop.class))).thenReturn(testShop);

        // When
        shopService.deleteShop(testShop.getId(), "owner@example.com");

        // Then
        verify(shopRepository).save(argThat(shop -> !shop.getIsActive()));
    }

    @Test
    @DisplayName("Should handle null/missing optional fields")
    void shouldHandleOptionalFields() {
        // Given
        CreateShopRequest minimalRequest = CreateShopRequest.builder()
                .name("Minimal Shop")
                .maxQueueSize(30)
                .avgServiceTimeMinutes(10)
                .build();

        Shop savedShop = Shop.builder()
                .id(UUID.randomUUID())
                .owner(testOwner)
                .name("Minimal Shop")
                .maxQueueSize(30)
                .avgServiceTimeMinutes(10)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testOwner));
        when(shopRepository.save(any(Shop.class))).thenReturn(savedShop);

        // When
        ShopResponse response = shopService.createShop(minimalRequest, "owner@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Minimal Shop");
    }

    @Test
    @DisplayName("Should preserve shop details on update")
    void shouldPreserveShopDetailsOnPartialUpdate() {
        // Given
        UpdateShopRequest partialUpdate = UpdateShopRequest.builder()
                .name("New Name")
                // description, address, etc. are null
                .build();

        Shop shopWithDetails = testShop;
        shopWithDetails.setDescription("Original Description");
        shopWithDetails.setAddress("Original Address");

        when(shopRepository.findById(testShop.getId())).thenReturn(Optional.of(shopWithDetails));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testOwner));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(zSetOperations.zCard(any())).thenReturn(0L);

        // When
        ShopResponse response = shopService.updateShop(testShop.getId(), partialUpdate, "owner@example.com");

        // Then
        assertThat(response.getName()).isEqualTo("New Name");
        // Original values should be preserved
        assertThat(response.getDescription()).isEqualTo("Original Description");
        assertThat(response.getAddress()).isEqualTo("Original Address");
    }

    @Test
    @DisplayName("Should include current queue size in response")
    void shouldIncludeCurrentQueueSizeInResponse() {
        // Given
        when(shopRepository.findById(testShop.getId())).thenReturn(Optional.of(testShop));
        when(zSetOperations.zCard("queue:" + testShop.getId())).thenReturn(5L);

        // When
        ShopResponse response = shopService.getShopById(testShop.getId());

        // Then
        assertThat(response.getCurrentQueueSize()).isEqualTo(5);
    }
}
