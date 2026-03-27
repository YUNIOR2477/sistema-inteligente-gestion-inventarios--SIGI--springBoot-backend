package com.sigi.services;

import com.sigi.persistence.entity.Dispatcher;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.DispatcherRepository;
import com.sigi.persistence.repository.MovementRepository;
import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.dispatcher.NewDispatcherDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.dispatcher.DispatcherServiceImpl;
import com.sigi.services.service.websocket.notification.NotificationService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.sigi.util.Constants.*;

@ExtendWith(MockitoExtension.class)
class DispatcherServiceImplTest {

    @Mock
    private DispatcherRepository dispatcherRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DispatcherServiceImpl dispatcherService;

    private SimpleMeterRegistry meterRegistry;

    private Dispatcher dispatcher;
    private DispatcherDto dispatcherDto;
    private NewDispatcherDto newDispatcherDto;
    private UUID dispatcherId;
    private User adminUser;
    private User warehouseUser;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dispatcherService = new DispatcherServiceImpl(dispatcherRepository, movementRepository, persistenceMethod, dtoMapper, meterRegistry, notificationService);

        dispatcherId = UUID.randomUUID();
        adminUser = User.builder().id(UUID.randomUUID()).email("admin@example.com").build();
        warehouseUser = User.builder().id(UUID.randomUUID()).email("warehouse@example.com").build();

        dispatcher = Dispatcher.builder()
                .id(dispatcherId)
                .name("Fast Dispatch")
                .identification("DISP-001")
                .contact("Carlos")
                .phone("+573001234567")
                .location("Calle 1")
                .email("disp@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build();

        dispatcherDto = DispatcherDto.builder()
                .id(dispatcherId)
                .name(dispatcher.getName())
                .identification(dispatcher.getIdentification())
                .contact(dispatcher.getContact())
                .phone(dispatcher.getPhone())
                .location(dispatcher.getLocation())
                .email(dispatcher.getEmail())
                .active(dispatcher.getActive())
                .createdAt(dispatcher.getCreatedAt())
                .createdBy(dispatcher.getCreatedBy())
                .build();

        newDispatcherDto = NewDispatcherDto.builder()
                .name("Fast Dispatch")
                .identification("DISP-001")
                .contact("Carlos")
                .phone("+573001234567")
                .location("Calle 1")
                .email("disp@example.com")
                .build();

        // mapeos por defecto
        lenient().when(dtoMapper.toDispatcherDto(any(Dispatcher.class))).thenReturn(dispatcherDto);
        lenient().when(dtoMapper.toDispatcherDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<Dispatcher> page = inv.getArgument(0);
            return page.map(d -> dispatcherDto);
        });
    }

    // ------------------- createDispatcher -------------------
    @Test
    void shouldCreateDispatcherAndNotifyAdminsAndWarehouses() {
        when(dispatcherRepository.save(any(Dispatcher.class))).thenAnswer(inv -> {
            Dispatcher d = inv.getArgument(0);
            d.setId(dispatcherId);
            return d;
        });
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(warehouseUser));

        ApiResponse<DispatcherDto> response = dispatcherService.createDispatcher(newDispatcherDto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(DISPATCHER), response.getMessage());
        verify(dispatcherRepository, times(1)).save(any(Dispatcher.class));
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> userCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService, times(2)).createNotification(titleCaptor.capture(), bodyCaptor.capture(), userCaptor.capture());
        List<String> titles = titleCaptor.getAllValues();
        assertTrue(titles.stream().allMatch(t -> t.contains("New Dispatcher Created")));
        assertTrue(meterRegistry.get("dispatcher.service.operations").tags("type", "createDispatcher").counter().count() >= 1.0);
    }

    // ------------------- updateDispatcher -------------------
    @Test
    void shouldUpdateDispatcherSuccessfully() {
        Dispatcher existing = Dispatcher.builder().id(dispatcherId).active(true).name("Old").build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(existing);
        when(dispatcherRepository.save(existing)).thenReturn(existing);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(warehouseUser));

        ApiResponse<DispatcherDto> response = dispatcherService.updateDispatcher(dispatcherId, newDispatcherDto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(DISPATCHER), response.getMessage());
        verify(dispatcherRepository, times(1)).save(existing);
        verify(notificationService, times(2)).createNotification(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenUpdatingInactiveDispatcher() {
        Dispatcher inactive = Dispatcher.builder().id(dispatcherId).active(false).build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> dispatcherService.updateDispatcher(dispatcherId, newDispatcherDto));
        verify(dispatcherRepository, never()).save(any());
    }

    // ------------------- deleteDispatcher -------------------
    @Test
    void shouldDeleteDispatcherSuccessfullyWhenNoActiveMovements() {
        Dispatcher existing = Dispatcher.builder().id(dispatcherId).active(true).name("ToDelete").build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(existing);
        when(movementRepository.existsByDispatcherIdAndActiveTrue(dispatcherId)).thenReturn(false);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(warehouseUser));
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("deleter@example.com");
        when(dispatcherRepository.save(existing)).thenReturn(existing);

        ApiResponse<Void> response = dispatcherService.deleteDispatcher(dispatcherId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(DISPATCHER), response.getMessage());
        assertFalse(existing.getActive());
        assertNotNull(existing.getDeletedAt());
        assertEquals("deleter@example.com", existing.getDeletedBy());
        verify(notificationService, times(2)).createNotification(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenDeletingDispatcherWithActiveMovements() {
        Dispatcher existing = Dispatcher.builder().id(dispatcherId).active(true).build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(existing);
        when(movementRepository.existsByDispatcherIdAndActiveTrue(dispatcherId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> dispatcherService.deleteDispatcher(dispatcherId));
        verify(dispatcherRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDeletingAlreadyInactiveDispatcher() {
        Dispatcher inactive = Dispatcher.builder().id(dispatcherId).active(false).build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> dispatcherService.deleteDispatcher(dispatcherId));
        verify(dispatcherRepository, never()).save(any());
    }

    // ------------------- restoreDispatcher -------------------
    @Test
    void shouldRestoreDispatcherSuccessfully() {
        Dispatcher inactive = Dispatcher.builder().id(dispatcherId).active(false).name("Restorable").build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(inactive);
        when(dispatcherRepository.existsByNameAndActiveTrue(inactive.getName())).thenReturn(false);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(adminUser));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(warehouseUser));
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("restorer@example.com");
        when(dispatcherRepository.save(inactive)).thenReturn(inactive);

        ApiResponse<Void> response = dispatcherService.restoreDispatcher(dispatcherId);

        assertEquals(200, response.getCode());
        assertTrue(inactive.getActive());
        assertNull(inactive.getDeletedAt());
        assertNull(inactive.getDeletedBy());
        verify(notificationService, times(2)).createNotification(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenRestoringAlreadyActiveDispatcher() {
        Dispatcher active = Dispatcher.builder().id(dispatcherId).active(true).build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(active);

        assertThrows(IllegalArgumentException.class, () -> dispatcherService.restoreDispatcher(dispatcherId));
        verify(dispatcherRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRestoringDispatcherWithNameConflict() {
        Dispatcher inactive = Dispatcher.builder().id(dispatcherId).active(false).name("Conflict").build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(inactive);
        when(dispatcherRepository.existsByNameAndActiveTrue("Conflict")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> dispatcherService.restoreDispatcher(dispatcherId));
        verify(dispatcherRepository, never()).save(any());
    }

    // ------------------- getDispatcherById -------------------
    @Test
    void shouldGetDispatcherByIdSuccessfully() {
        Dispatcher existing = Dispatcher.builder().id(dispatcherId).active(true).build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(existing);

        ApiResponse<DispatcherDto> response = dispatcherService.getDispatcherById(dispatcherId);

        assertEquals(200, response.getCode());
        assertEquals(dispatcherDto, response.getData());
    }

    @Test
    void shouldThrowWhenGettingInactiveDispatcherById() {
        Dispatcher inactive = Dispatcher.builder().id(dispatcherId).active(false).build();
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> dispatcherService.getDispatcherById(dispatcherId));
    }

    // ------------------- getDeletedDispatcherById -------------------
    @Test
    void shouldGetDeletedDispatcherByIdSuccessfully() {
        Dispatcher deleted = Dispatcher.builder().id(dispatcherId).active(false).build();
        when(dispatcherRepository.findByIdAndActiveFalse(dispatcherId)).thenReturn(Optional.of(deleted));

        ApiResponse<DispatcherDto> response = dispatcherService.getDeletedDispatcherById(dispatcherId);

        assertEquals(200, response.getCode());
        assertEquals(dispatcherDto, response.getData());
    }

    @Test
    void shouldThrowWhenDeletedDispatcherNotFound() {
        when(dispatcherRepository.findByIdAndActiveFalse(dispatcherId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> dispatcherService.getDeletedDispatcherById(dispatcherId));
    }

    // ------------------- listAllActiveDispatchers -------------------
    @Test
    void shouldListAllActiveDispatchersSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Dispatcher> page = new PageImpl<>(List.of(dispatcher), PageRequest.of(0,10), 1);
        when(dispatcherRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listAllActiveDispatchers(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- listAllDeletedDispatchers -------------------
    @Test
    void shouldListAllDeletedDispatchersSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Dispatcher> page = new PageImpl<>(List.of(dispatcher));
        when(dispatcherRepository.findByActiveFalse(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listAllDeletedDispatchers(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- listDispatcherByName -------------------
    @Test
    void shouldListDispatcherByNameSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("Fast").sortDirection("desc").sortField("createdAt").build();
        Page<Dispatcher> page = new PageImpl<>(List.of(dispatcher));
        when(dispatcherRepository.findByNameContainingIgnoreCaseAndActiveTrue(eq("Fast"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listDispatcherByName(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- listDeletedDispatcherByName -------------------
    @Test
    void shouldListDeletedDispatcherByNameSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("Fast").sortDirection("desc").sortField("createdAt").build();
        Page<Dispatcher> page = new PageImpl<>(List.of(dispatcher));
        when(dispatcherRepository.findByNameContainingIgnoreCaseAndActiveFalse(eq("Fast"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listDeletedDispatcherByName(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }
}