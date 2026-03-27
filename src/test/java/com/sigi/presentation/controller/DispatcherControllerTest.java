package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.dispatcher.NewDispatcherDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.dispatcher.DispatcherService;
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


@WebMvcTest(controllers = DispatcherController.class)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class DispatcherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DispatcherService dispatcherService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private DispatcherDto sampleDispatcher;
    private UUID dispatcherId;

    @BeforeEach
    void setUp() {
        dispatcherId = UUID.randomUUID();
        sampleDispatcher = DispatcherDto.builder()
                .id(dispatcherId)
                .name("Fast Delivery Co")
                .contact("Carlos Gómez")
                .phone("+573001234567")
                .location("Calle 1 #2-3")
                .identification("DISP-10234")
                .email("dispatcher@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createDispatcher_success_returnsCreatedDispatcher() throws Exception {
        NewDispatcherDto request = NewDispatcherDto.builder()
                .name("Fast Delivery Co")
                .identification("DISP-10234")
                .contact("Carlos Gómez")
                .location("Calle 1 #2-3")
                .phone("+573001234567")
                .email("dispatcher@example.com")
                .build();

        when(dispatcherService.createDispatcher(any(NewDispatcherDto.class)))
                .thenReturn(ApiResponse.success("Dispatcher created", sampleDispatcher));

        mockMvc.perform(post("/api/v1/dispatchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(dispatcherId.toString()))
                .andExpect(jsonPath("$.data.name").value("Fast Delivery Co"));

        ArgumentCaptor<NewDispatcherDto> captor = ArgumentCaptor.forClass(NewDispatcherDto.class);
        verify(dispatcherService, times(1)).createDispatcher(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Fast Delivery Co");
    }

    @Test
    void createDispatcher_validationFails_returnsBadRequest() throws Exception {
        // Missing required fields (name, identification, contact)
        NewDispatcherDto invalid = NewDispatcherDto.builder()
                .name("") // invalid
                .identification("") // invalid
                .contact("") // invalid
                .build();

        mockMvc.perform(post("/api/v1/dispatchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(dispatcherService, never()).createDispatcher(any());
    }

    @Test
    void updateDispatcher_success_delegatesToService() throws Exception {
        NewDispatcherDto request = NewDispatcherDto.builder()
                .name("Fast Delivery Updated")
                .identification("DISP-10234")
                .contact("Carlos Gómez")
                .location("New Location")
                .phone("+573001234567")
                .email("updated@example.com")
                .build();

        DispatcherDto updated = DispatcherDto.builder()
                .id(dispatcherId)
                .name("Fast Delivery Updated")
                .identification("DISP-10234")
                .contact("Carlos Gómez")
                .location("New Location")
                .email("updated@example.com")
                .build();

        when(dispatcherService.updateDispatcher(eq(dispatcherId), any(NewDispatcherDto.class)))
                .thenReturn(ApiResponse.success("Dispatcher updated", updated));

        mockMvc.perform(put("/api/v1/dispatchers/{id}", dispatcherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Fast Delivery Updated"));

        verify(dispatcherService, times(1)).updateDispatcher(eq(dispatcherId), any(NewDispatcherDto.class));
    }

    @Test
    void deleteDispatcher_success_delegatesToService() throws Exception {
        when(dispatcherService.deleteDispatcher(dispatcherId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/dispatchers/{id}", dispatcherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        verify(dispatcherService, times(1)).deleteDispatcher(dispatcherId);
    }

    @Test
    void restoreDispatcher_success_delegatesToService() throws Exception {
        when(dispatcherService.restoreDispatcher(dispatcherId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(put("/api/v1/dispatchers/{id}/restore", dispatcherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored"));

        verify(dispatcherService, times(1)).restoreDispatcher(dispatcherId);
    }

    @Test
    void getDispatcherById_returnsDispatcher() throws Exception {
        when(dispatcherService.getDispatcherById(dispatcherId)).thenReturn(ApiResponse.success("Found", sampleDispatcher));

        mockMvc.perform(get("/api/v1/dispatchers/{id}", dispatcherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(dispatcherId.toString()))
                .andExpect(jsonPath("$.data.identification").value("DISP-10234"));

        verify(dispatcherService, times(1)).getDispatcherById(dispatcherId);
    }

    @Test
    void getDeletedDispatcherById_returnsDispatcher() throws Exception {
        when(dispatcherService.getDeletedDispatcherById(dispatcherId)).thenReturn(ApiResponse.success("Found deleted", sampleDispatcher));

        mockMvc.perform(get("/api/v1/dispatchers/deleted/{id}", dispatcherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Found deleted"))
                .andExpect(jsonPath("$.data.id").value(dispatcherId.toString()));

        verify(dispatcherService, times(1)).getDeletedDispatcherById(dispatcherId);
    }

    @Test
    void listAllActiveDispatchers_returnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleDispatcher), PageRequest.of(0, 10), 1);
        when(dispatcherService.listAllActiveDispatchers(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/dispatchers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(dispatcherId.toString()));

        verify(dispatcherService, times(1)).listAllActiveDispatchers(any(PagedRequestDto.class));
    }

    @Test
    void listAllDeletedDispatchers_returnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleDispatcher), PageRequest.of(0, 5), 1);
        when(dispatcherService.listAllDeletedDispatchers(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/dispatchers/deleted")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(dispatcherId.toString()));

        verify(dispatcherService, times(1)).listAllDeletedDispatchers(any(PagedRequestDto.class));
    }

    @Test
    void listDispatcherByName_delegatesAndReturnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleDispatcher), PageRequest.of(0, 10), 1);
        when(dispatcherService.listDispatcherByName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/dispatchers/by-name")
                        .param("searchValue", "Fast")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Fast Delivery Co"));

        ArgumentCaptor<PagedRequestDto> captor = ArgumentCaptor.forClass(PagedRequestDto.class);
        verify(dispatcherService, times(1)).listDispatcherByName(captor.capture());
        assertThat(captor.getValue().getSearchValue()).isEqualTo("Fast");
    }

    @Test
    void listDeletedByName_delegatesAndReturnsPaged() throws Exception {
        var page = new PageImpl<>(List.of(sampleDispatcher), PageRequest.of(0, 10), 1);
        when(dispatcherService.listDeletedDispatcherByName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/dispatchers/deleted/by-name")
                        .param("searchValue", "Fast")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Fast Delivery Co"));

        verify(dispatcherService, times(1)).listDeletedDispatcherByName(any(PagedRequestDto.class));
    }
}