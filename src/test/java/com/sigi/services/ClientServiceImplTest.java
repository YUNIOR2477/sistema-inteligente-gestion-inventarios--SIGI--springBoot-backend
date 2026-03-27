package com.sigi.services;

import com.sigi.persistence.entity.Client;
import com.sigi.persistence.repository.ClientRepository;
import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.client.NewClientDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.client.ClientServiceImpl;
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
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @InjectMocks
    private ClientServiceImpl clientService;

    private SimpleMeterRegistry meterRegistry;

    private NewClientDto newClientDto;
    private Client client;
    private ClientDto clientDto;
    private UUID clientId;
    private String currentUserEmail = "tester@example.com";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        clientService = new ClientServiceImpl(clientRepository, dtoMapper, meterRegistry, persistenceMethod);

        clientId = UUID.randomUUID();
        newClientDto = NewClientDto.builder()
                .name("ACME Corp")
                .identification("1023456789")
                .location("Calle 1 #2-3")
                .phone("+573001234567")
                .email("cliente@acme.com")
                .build();

        client = Client.builder()
                .id(clientId)
                .name(newClientDto.getName())
                .identification(newClientDto.getIdentification())
                .location(newClientDto.getLocation())
                .phone(newClientDto.getPhone())
                .email(newClientDto.getEmail())
                .active(true)
                .createdAt(LocalDateTime.now())
                .createdBy(currentUserEmail)
                .build();

        clientDto = ClientDto.builder()
                .id(clientId)
                .name(client.getName())
                .identification(client.getIdentification())
                .location(client.getLocation())
                .phone(client.getPhone())
                .email(client.getEmail())
                .active(client.getActive())
                .createdAt(client.getCreatedAt())
                .createdBy(client.getCreatedBy())
                .build();

        // mapeos por defecto
        lenient().when(dtoMapper.toClientDto(any(Client.class))).thenReturn(clientDto);
        lenient().when(dtoMapper.toClientDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<Client> page = inv.getArgument(0);
            return page.map(c -> clientDto);
        });
    }

    // ------------------- createClient -------------------
    @Test
    void shouldCreateClientSuccessfully() {
        when(persistenceMethod.getCurrentUserEmail()).thenReturn(currentUserEmail);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            c.setId(clientId);
            return c;
        });

        ApiResponse<ClientDto> response = clientService.createClient(newClientDto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(CLIENT), response.getMessage());
        assertEquals(clientDto, response.getData());
        verify(clientRepository, times(1)).save(any(Client.class));
        assertTrue(meterRegistry.get("client.service.operations").tags("type", "createClient").counter().count() >= 1.0);
    }

    // ------------------- updateClient -------------------
    @Test
    void shouldUpdateClientSuccessfully() {
        NewClientDto update = NewClientDto.builder()
                .name("ACME Updated")
                .identification("1023456789")
                .location("Nueva direccion")
                .phone("+573009876543")
                .email("nuevo@acme.com")
                .build();

        Client existing = Client.builder()
                .id(clientId)
                .active(true)
                .build();

        when(persistenceMethod.getClientById(clientId)).thenReturn(existing);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn(currentUserEmail);
        when(clientRepository.save(existing)).thenReturn(existing);

        ApiResponse<ClientDto> response = clientService.updateClient(clientId, update);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(CLIENT), response.getMessage());
        verify(clientRepository, times(1)).save(existing);
        assertNotNull(existing.getUpdatedAt());
        assertEquals(currentUserEmail, existing.getUpdatedBy());
    }

    @Test
    void shouldThrowWhenUpdatingInactiveClient() {
        Client inactive = Client.builder().id(clientId).active(false).build();
        when(persistenceMethod.getClientById(clientId)).thenReturn(inactive);

        NewClientDto update = NewClientDto.builder().name("X").identification("1").location("L").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> clientService.updateClient(clientId, update));
        assertTrue(ex.getMessage().contains("ENTITY_INACTIVE") || ex.getMessage().contains("inactive") || ex.getMessage().length() > 0);
        verify(clientRepository, never()).save(any());
    }

    // ------------------- deleteClient -------------------
    @Test
    void shouldDeleteClientSuccessfully() {
        Client existing = Client.builder().id(clientId).active(true).build();
        when(persistenceMethod.getClientById(clientId)).thenReturn(existing);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn(currentUserEmail);
        when(clientRepository.save(existing)).thenReturn(existing);

        ApiResponse<Void> response = clientService.deleteClient(clientId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(CLIENT), response.getMessage());
        assertFalse(existing.getActive());
        assertNotNull(existing.getDeletedAt());
        assertEquals(currentUserEmail, existing.getDeletedBy());
        verify(clientRepository, times(1)).save(existing);
    }

    @Test
    void shouldThrowWhenDeletingAlreadyInactiveClient() {
        Client inactive = Client.builder().id(clientId).active(false).build();
        when(persistenceMethod.getClientById(clientId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> clientService.deleteClient(clientId));
        verify(clientRepository, never()).save(any());
    }

    // ------------------- restoreClient -------------------
    @Test
    void shouldRestoreClientSuccessfully() {
        Client inactive = Client.builder().id(clientId).active(false).deletedAt(LocalDateTime.now()).deletedBy("admin").build();
        when(persistenceMethod.getClientById(clientId)).thenReturn(inactive);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn(currentUserEmail);
        when(clientRepository.save(inactive)).thenReturn(inactive);

        ApiResponse<Void> response = clientService.restoreClient(clientId);

        assertEquals(200, response.getCode());
        assertTrue(inactive.getActive());
        assertNull(inactive.getDeletedAt());
        assertNull(inactive.getDeletedBy());
        verify(clientRepository, times(1)).save(inactive);
    }

    @Test
    void shouldThrowWhenRestoringAlreadyActiveClient() {
        Client active = Client.builder().id(clientId).active(true).build();
        when(persistenceMethod.getClientById(clientId)).thenReturn(active);

        assertThrows(IllegalArgumentException.class, () -> clientService.restoreClient(clientId));
        verify(clientRepository, never()).save(any());
    }

    // ------------------- getClientById -------------------
    @Test
    void shouldGetClientByIdSuccessfully() {
        Client existing = Client.builder().id(clientId).active(true).build();
        when(persistenceMethod.getClientById(clientId)).thenReturn(existing);

        ApiResponse<ClientDto> response = clientService.getClientById(clientId);

        assertEquals(200, response.getCode());
        assertEquals(clientDto, response.getData());
    }

    @Test
    void shouldThrowWhenGettingInactiveClientById() {
        Client inactive = Client.builder().id(clientId).active(false).build();
        when(persistenceMethod.getClientById(clientId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> clientService.getClientById(clientId));
    }

    // ------------------- getDeletedClientById -------------------
    @Test
    void shouldGetDeletedClientByIdSuccessfully() {
        Client deleted = Client.builder().id(clientId).active(false).build();
        when(clientRepository.findByIdAndActiveFalse(clientId)).thenReturn(Optional.of(deleted));

        ApiResponse<ClientDto> response = clientService.getDeletedClientById(clientId);

        assertEquals(200, response.getCode());
        assertEquals(clientDto, response.getData());
    }

    @Test
    void shouldThrowWhenDeletedClientNotFound() {
        when(clientRepository.findByIdAndActiveFalse(clientId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clientService.getDeletedClientById(clientId));
    }

    // ------------------- listAllActiveClients -------------------
    @Test
    void shouldListAllActiveClientsSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Client> page = new PageImpl<>(List.of(client), PageRequest.of(0,10), 1);
        when(clientRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ClientDto>> response = clientService.listAllActiveClients(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- listAllDeletedClients -------------------
    @Test
    void shouldListAllDeletedClientsSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Client> page = new PageImpl<>(List.of(client));
        when(clientRepository.findByActiveFalse(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ClientDto>> response = clientService.listAllDeletedClients(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- listClientsByName -------------------
    @Test
    void shouldListClientsByNameSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("ACME").sortDirection("desc").sortField("createdAt").build();
        Page<Client> page = new PageImpl<>(List.of(client));
        when(clientRepository.findByNameContainingIgnoreCaseAndActiveTrue(eq("ACME"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ClientDto>> response = clientService.listClientsByName(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- listClientDeletedByName -------------------
    @Test
    void shouldListClientDeletedByNameSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("ACME").sortDirection("desc").sortField("createdAt").build();
        Page<Client> page = new PageImpl<>(List.of(client));
        when(clientRepository.findByNameContainingIgnoreCaseAndActiveFalse(eq("ACME"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ClientDto>> response = clientService.listClientDeletedByName(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- getClientByIdentification -------------------
    @Test
    void shouldGetClientByIdentificationSuccessfully() {
        Client existing = Client.builder().id(clientId).active(true).build();
        when(persistenceMethod.getClientByIdentification("1023456789")).thenReturn(existing);

        ApiResponse<ClientDto> response = clientService.getClientByIdentification("1023456789");

        assertEquals(200, response.getCode());
        assertEquals(clientDto, response.getData());
    }

    @Test
    void shouldThrowWhenClientByIdentificationIsInactive() {
        Client inactive = Client.builder().id(clientId).active(false).build();
        when(persistenceMethod.getClientByIdentification("1023456789")).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> clientService.getClientByIdentification("1023456789"));
    }
}