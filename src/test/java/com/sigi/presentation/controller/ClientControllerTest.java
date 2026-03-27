package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.client.NewClientDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.client.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private ClientService clientService;

    @Autowired
    private ObjectMapper objectMapper;

    private ClientDto sampleClient;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        sampleClient = ClientDto.builder()
                .id(clientId)
                .name("Acme Corp")
                .identification("1023456789")
                .location("Calle 10 # 5-20, Armenia")
                .phone("+573001234567")
                .email("cliente@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createClient_success_returnsCreatedClient() throws Exception {
        NewClientDto request = NewClientDto.builder()
                .name("Acme Corp")
                .identification("1023456789")
                .location("Calle 10 # 5-20, Armenia")
                .phone("+573001234567")
                .email("cliente@example.com")
                .build();

        when(clientService.createClient(any(NewClientDto.class)))
                .thenReturn(ApiResponse.success("Client created", sampleClient));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(clientId.toString()))
                .andExpect(jsonPath("$.data.name").value("Acme Corp"));

        ArgumentCaptor<NewClientDto> captor = ArgumentCaptor.forClass(NewClientDto.class);
        verify(clientService, times(1)).createClient(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Acme Corp");
    }

    @Test
    void createClient_validationFails_returnsBadRequest() throws Exception {
        // Missing required fields (name and identification)
        NewClientDto invalid = NewClientDto.builder()
                .name("") // invalid
                .identification("") // invalid
                .location("Somewhere")
                .build();

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(clientService, never()).createClient(any());
    }

    @Test
    void updateClient_success_delegatesToService() throws Exception {
        NewClientDto request = NewClientDto.builder()
                .name("Acme Updated")
                .identification("1023456789")
                .location("New Location")
                .phone("+573001234567")
                .email("updated@example.com")
                .build();

        ClientDto updated = ClientDto.builder()
                .id(clientId)
                .name("Acme Updated")
                .identification("1023456789")
                .location("New Location")
                .email("updated@example.com")
                .build();

        when(clientService.updateClient(eq(clientId), any(NewClientDto.class)))
                .thenReturn(ApiResponse.success("Client updated", updated));

        mockMvc.perform(put("/api/v1/clients/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Acme Updated"));

        verify(clientService, times(1)).updateClient(eq(clientId), any(NewClientDto.class));
    }

    @Test
    void deleteClient_success_delegatesToService() throws Exception {
        when(clientService.deleteClient(clientId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/clients/{id}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        verify(clientService, times(1)).deleteClient(clientId);
    }

    @Test
    void restoreClient_success_delegatesToService() throws Exception {
        when(clientService.restoreClient(clientId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(put("/api/v1/clients/{id}/restore", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored"));

        verify(clientService, times(1)).restoreClient(clientId);
    }

    @Test
    void getClientById_returnsClient() throws Exception {
        when(clientService.getClientById(clientId)).thenReturn(ApiResponse.success("Found", sampleClient));

        mockMvc.perform(get("/api/v1/clients/{id}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(clientId.toString()))
                .andExpect(jsonPath("$.data.identification").value("1023456789"));

        verify(clientService, times(1)).getClientById(clientId);
    }

    @Test
    void getDeletedClientById_returnsClient() throws Exception {
        when(clientService.getDeletedClientById(clientId)).thenReturn(ApiResponse.success("Found deleted", sampleClient));

        mockMvc.perform(get("/api/v1/clients/deleted/{id}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Found deleted"))
                .andExpect(jsonPath("$.data.id").value(clientId.toString()));

        verify(clientService, times(1)).getDeletedClientById(clientId);
    }

    @Test
    void listAllActiveClients_returnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleClient), PageRequest.of(0, 10), 1);
        when(clientService.listAllActiveClients(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/clients")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(clientId.toString()));

        verify(clientService, times(1)).listAllActiveClients(any(PagedRequestDto.class));
    }

    @Test
    void listAllDeletedClients_returnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleClient), PageRequest.of(0, 5), 1);
        when(clientService.listAllDeletedClients(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/clients/deleted")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(clientId.toString()));

        verify(clientService, times(1)).listAllDeletedClients(any(PagedRequestDto.class));
    }

    @Test
    void getClientByIdentification_delegatesAndReturns() throws Exception {
        when(clientService.getClientByIdentification("1023456789"))
                .thenReturn(ApiResponse.success("Found", sampleClient));

        mockMvc.perform(get("/api/v1/clients/by-identification")
                        .param("identification", "1023456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identification").value("1023456789"));

        verify(clientService, times(1)).getClientByIdentification("1023456789");
    }

    @Test
    void listClientsByName_delegatesAndReturnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleClient), PageRequest.of(0, 10), 1);
        when(clientService.listClientsByName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/clients/by-name")
                        .param("searchValue", "Acme")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Acme Corp"));

        ArgumentCaptor<PagedRequestDto> captor = ArgumentCaptor.forClass(PagedRequestDto.class);
        verify(clientService, times(1)).listClientsByName(captor.capture());
        assertThat(captor.getValue().getSearchValue()).isEqualTo("Acme");
    }

    @Test
    void listDeletedClientsByName_delegatesAndReturnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleClient), PageRequest.of(0, 10), 1);
        when(clientService.listClientDeletedByName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/clients/deleted/by-name")
                        .param("searchValue", "Acme")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Acme Corp"));

        verify(clientService, times(1)).listClientDeletedByName(any(PagedRequestDto.class));
    }
}