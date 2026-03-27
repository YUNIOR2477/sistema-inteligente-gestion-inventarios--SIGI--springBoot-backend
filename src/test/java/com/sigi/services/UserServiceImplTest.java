package com.sigi.services;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.*;
import com.sigi.presentation.dto.auth.NewUserDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.user.UpdateUserProfileDto;
import com.sigi.presentation.dto.user.UserDto;
import com.sigi.services.service.user.UserServiceImpl;
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.sigi.util.Constants.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private DtoMapper dtoMapper;
    @Mock private PersistenceMethod persistenceMethod;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    private SimpleMeterRegistry meterRegistry;
    private UUID userId;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        userService = new UserServiceImpl(userRepository, productRepository, orderRepository,
                inventoryRepository, warehouseRepository, dtoMapper, meterRegistry, persistenceMethod, passwordEncoder);

        userId = UUID.randomUUID();
        Role role = Role.builder().id(2L).name(com.sigi.persistence.enums.RoleList.ROLE_ADMIN).build();

        user = User.builder()
                .id(userId)
                .name("Carlos")
                .surname("Gomez")
                .email("carlos@example.com")
                .phoneNumber("+573001234567")
                .password("encoded")
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        userDto = UserDto.builder()
                .id(userId)
                .name(user.getName())
                .email(user.getEmail())
                .build();

        lenient().when(dtoMapper.toUserDto(any(User.class))).thenReturn(userDto);
        lenient().when(dtoMapper.toUserDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<User> page = inv.getArgument(0);
            return page.map(u -> userDto);
        });
    }

    // ------------------- createUser -------------------
    @Test
    void shouldCreateUserSuccessfully() {
        NewUserDto dto = NewUserDto.builder()
                .name("Carlos")
                .surname("Gomez")
                .email("carlos@example.com")
                .phoneNumber("+573001234567")
                .password("PlainPass123!")
                .role("ROLE_ADMIN")
                .build();

        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPass");
        when(persistenceMethod.getRoleByName(dto.getRole())).thenReturn(Role.builder().name(com.sigi.persistence.enums.RoleList.ROLE_ADMIN).build());
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("system");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        ApiResponse<UserDto> response = userService.createUser(dto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(USER), response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
        assertTrue(meterRegistry.get("user.service.operations").tags("type", "createUser").counter().count() >= 1.0);
    }

    // ------------------- updateUser -------------------
    @Test
    void shouldUpdateUserSuccessfullyWhenActive() {
        NewUserDto dto = NewUserDto.builder()
                .name("Carlos Updated")
                .surname("Gomez Updated")
                .email("carlos2@example.com")
                .phoneNumber("+573009999999")
                .password("NewPass123!")
                .role("ROLE_ADMIN")
                .build();

        when(persistenceMethod.getUserById(userId)).thenReturn(user);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedNew");
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("updater@example.com");
        when(userRepository.save(user)).thenReturn(user);

        ApiResponse<UserDto> response = userService.updateUser(userId, dto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(USER), response.getMessage());
        assertEquals("encodedNew", user.getPassword());
        assertNotNull(user.getUpdatedAt());
        assertEquals("updater@example.com", user.getUpdatedBy());
    }

    @Test
    void shouldThrowWhenUpdateUserInactive() {
        user.setActive(false);
        when(persistenceMethod.getUserById(userId)).thenReturn(user);

        NewUserDto dto = NewUserDto.builder().name("X").surname("Y").email("x@x.com").phoneNumber("123").password("p").role("ROLE_ADMIN").build();

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(userId, dto));
        verify(userRepository, never()).save(any());
    }

    // ------------------- updateUserProfile -------------------
    @Test
    void shouldUpdateUserProfileAndEncodePasswordWhenProvided() {
        UpdateUserProfileDto dto = UpdateUserProfileDto.builder()
                .name("Carlos P")
                .surname("Gomez P")
                .email("carlosp@example.com")
                .phoneNumber("+573001111111")
                .password("NewProfilePass!")
                .build();

        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedProfile");
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("me@example.com");
        when(userRepository.save(user)).thenReturn(user);

        ApiResponse<UserDto> response = userService.updateUserProfile(dto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(USER), response.getMessage());
        assertEquals("encodedProfile", user.getPassword());
        assertEquals("me@example.com", user.getUpdatedBy());
    }

    @Test
    void shouldUpdateUserProfileWithoutChangingPasswordWhenBlank() {
        UpdateUserProfileDto dto = UpdateUserProfileDto.builder()
                .name("Carlos P")
                .surname("Gomez P")
                .email("carlosp@example.com")
                .phoneNumber("+573001111111")
                .password("") // blank
                .build();

        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("me@example.com");
        when(userRepository.save(user)).thenReturn(user);

        ApiResponse<UserDto> response = userService.updateUserProfile(dto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(USER), response.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // ------------------- deactivate / activate -------------------
    @Test
    void shouldDeactivateUserSuccessfully() {
        when(persistenceMethod.getUserById(userId)).thenReturn(user);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("admin@example.com");
        when(userRepository.save(user)).thenReturn(user);

        ApiResponse<Void> response = userService.deactivateUser(userId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(USER), response.getMessage());
        assertFalse(user.getActive());
        assertNotNull(user.getDeletedAt());
        assertEquals("admin@example.com", user.getDeletedBy());
    }

    @Test
    void shouldThrowWhenDeactivateAlreadyInactive() {
        user.setActive(false);
        when(persistenceMethod.getUserById(userId)).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () -> userService.deactivateUser(userId));
    }

    @Test
    void shouldActivateUserSuccessfully() {
        User deleted = User.builder().id(userId).active(false).build();
        when(persistenceMethod.getUserById(userId)).thenReturn(deleted);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("admin@example.com");
        when(userRepository.save(deleted)).thenReturn(deleted);

        ApiResponse<Void> response = userService.activateUser(userId);

        assertEquals(200, response.getCode());
        assertTrue(deleted.getActive());
        assertNull(deleted.getDeletedAt());
    }

    @Test
    void shouldThrowWhenActivateAlreadyActive() {
        when(persistenceMethod.getUserById(userId)).thenReturn(user);
        assertThrows(IllegalArgumentException.class, () -> userService.activateUser(userId));
    }

    // ------------------- getUserById / getDeletedUserById -------------------
    @Test
    void shouldGetUserByIdWhenActive() {
        when(persistenceMethod.getUserById(userId)).thenReturn(user);

        ApiResponse<UserDto> response = userService.getUserById(userId);

        assertEquals(200, response.getCode());
        assertEquals(userDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetUserByIdIfInactive() {
        user.setActive(false);
        when(persistenceMethod.getUserById(userId)).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(userId));
    }

    @Test
    void shouldGetDeletedUserByIdSuccessfully() {
        User deleted = User.builder().id(userId).active(false).build();
        when(userRepository.findByIdAndActiveFalse(userId)).thenReturn(Optional.of(deleted));

        ApiResponse<UserDto> response = userService.getDeletedUserById(userId);

        assertEquals(200, response.getCode());
        verify(userRepository, times(1)).findByIdAndActiveFalse(userId);
    }

    @Test
    void shouldThrowWhenDeletedUserNotFound() {
        when(userRepository.findByIdAndActiveFalse(userId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.getDeletedUserById(userId));
    }

    // ------------------- listActiveUsers / listInactiveUsers -------------------
    @Test
    void shouldListActiveUsersSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0,10), 1);
        when(userRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<UserDto>> response = userService.listActiveUsers(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- changeStatusNotification / changeStatusNotificationChat -------------------
    @Test
    void shouldToggleNotifications() {
        User current = User.builder().id(userId).notificationsEnabled(true).build();
        when(persistenceMethod.getCurrentUser()).thenReturn(current);
        when(userRepository.save(current)).thenReturn(current);

        ApiResponse<Void> response = userService.changeStatusNotification();

        assertEquals(200, response.getCode());
        assertFalse(current.isNotificationsEnabled());

        // toggle back
        when(persistenceMethod.getCurrentUser()).thenReturn(current);
        current.setNotificationsEnabled(false);
        ApiResponse<Void> response2 = userService.changeStatusNotification();

        assertEquals(200, response2.getCode());
        assertTrue(current.isNotificationsEnabled());
    }

    @Test
    void shouldToggleChatNotifications() {
        User current = User.builder().id(userId).chatNotificationsEnabled(true).build();
        when(persistenceMethod.getCurrentUser()).thenReturn(current);
        when(userRepository.save(current)).thenReturn(current);

        ApiResponse<Void> response = userService.changeStatusNotificationChat();

        assertEquals(200, response.getCode());
        assertFalse(current.isChatNotificationsEnabled());
    }

    // ------------------- changeRole -------------------
    @Test
    void shouldChangeRoleWhenUserActive() {
        when(persistenceMethod.getUserById(userId)).thenReturn(user);
        when(persistenceMethod.getRoleByName(RoleList.ROLE_WAREHOUSE.name())).thenReturn(Role.builder().name(RoleList.ROLE_WAREHOUSE).build());
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("admin@example.com");
        when(userRepository.save(user)).thenReturn(user);

        ApiResponse<Void> response = userService.changeRole(userId, RoleList.ROLE_WAREHOUSE);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted("Change role"), response.getMessage());
        assertEquals(RoleList.ROLE_WAREHOUSE, user.getRole().getName());
    }

    @Test
    void shouldThrowWhenChangeRoleUserInactive() {
        user.setActive(false);
        when(persistenceMethod.getUserById(userId)).thenReturn(user);

        assertThrows(IllegalArgumentException.class, () -> userService.changeRole(userId, RoleList.ROLE_ADMIN));
    }

    // ------------------- getMetrics -------------------
    @Test
    void shouldReturnMetricsDto() {
        // prepare lists
        Order o1 = Order.builder().status(com.sigi.persistence.enums.OrderStatus.PENDING).build();
        Order o2 = Order.builder().status(com.sigi.persistence.enums.OrderStatus.CANCELED).build();
        Product p1 = Product.builder().id(UUID.randomUUID()).build();
        Inventory inv1 = Inventory.builder().availableQuantity(BigDecimal.valueOf(5)).build();
        Inventory inv2 = Inventory.builder().availableQuantity(BigDecimal.valueOf(20)).build();

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));
        when(productRepository.findAll()).thenReturn(List.of(p1));
        when(inventoryRepository.findAll()).thenReturn(List.of(inv1, inv2));
        when(warehouseRepository.count()).thenReturn(2L);
        when(dtoMapper.toProductDtoList(anyList())).thenReturn(List.of());

        ApiResponse<com.sigi.presentation.dto.user.MetricsDto> response = userService.getMetrics();

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getTotalProducts());
        assertEquals(2, response.getData().getTotalWarehouses());
        assertEquals(1, response.getData().getTotalLoWStock()); // inv1 <= 10
    }
}