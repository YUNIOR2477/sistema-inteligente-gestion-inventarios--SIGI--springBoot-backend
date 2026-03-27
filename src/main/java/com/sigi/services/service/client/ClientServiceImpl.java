package com.sigi.services.service.client;

import com.sigi.persistence.entity.Client;
import com.sigi.persistence.repository.ClientRepository;
import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.client.NewClientDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;

    @Transactional
    @Override
    @CacheEvict(value = {CLIENT_BY_ID, CLIENTS_BY_NAME, CLIENT_BY_IDENTIFICATION, ALL_ACTIVE_CLIENTS, ALL_DELETED_CLIENTS, CLIENTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<ClientDto> createClient(NewClientDto newClient) {
        log.debug("(createClient) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client response = clientRepository.save(Client.builder()
                .name(newClient.getName())
                .identification(newClient.getIdentification())
                .location(newClient.getLocation())
                .phone(newClient.getPhone())
                .email(newClient.getEmail())
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build());
        recordMetrics(sample, "createClient");
        log.info("(createClient) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(CLIENT),
                dtoMapper.toClientDto(response));
    }

    @Transactional
    @Override
    @CacheEvict(value = {CLIENT_BY_ID, CLIENTS_BY_NAME, CLIENT_BY_IDENTIFICATION, ALL_ACTIVE_CLIENTS, ALL_DELETED_CLIENTS, CLIENTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<ClientDto> updateClient(UUID id, NewClientDto updateClient) {
        log.debug("(updateClient) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client existingClient = persistenceMethod.getClientById(id);
        if (existingClient.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(CLIENT_ID.formatted(id)));
        }
        existingClient.setName(updateClient.getName());
        existingClient.setIdentification(updateClient.getIdentification());
        existingClient.setLocation(updateClient.getLocation());
        existingClient.setPhone(updateClient.getPhone());
        existingClient.setEmail(updateClient.getEmail());
        existingClient.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingClient.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        Client response = clientRepository.save(existingClient);
        recordMetrics(sample, "updateClient");
        log.info("(updateClient) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(CLIENT),
                dtoMapper.toClientDto(response));
    }

    @Transactional
    @Override
    @CacheEvict(value = {CLIENT_BY_ID, CLIENTS_BY_NAME, CLIENT_BY_IDENTIFICATION, ALL_ACTIVE_CLIENTS, ALL_DELETED_CLIENTS, CLIENTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> deleteClient(UUID id) {
        log.debug("(deleteClient) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client existingClient = persistenceMethod.getClientById(id);
        if (existingClient.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_PREVIOUSLY_DELETED.formatted(CLIENT_ID.formatted(id)));
        }
        existingClient.setActive(false);
        existingClient.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        existingClient.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingClient.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingClient.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        clientRepository.save(existingClient);
        recordMetrics(sample, "deleteClient");
        log.info("(deleteClient) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(CLIENT), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {CLIENT_BY_ID, CLIENTS_BY_NAME, CLIENT_BY_IDENTIFICATION, ALL_ACTIVE_CLIENTS, ALL_DELETED_CLIENTS, CLIENTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> restoreClient(UUID id) {
        log.debug("(restoreClient) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client existingClient = persistenceMethod.getClientById(id);
        if (Boolean.TRUE.equals(existingClient.getActive())) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(CLIENT_ID.formatted(id)));
        }
        existingClient.setActive(true);
        existingClient.setDeletedAt(null);
        existingClient.setDeletedBy(null);
        existingClient.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingClient.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        clientRepository.save(existingClient);
        recordMetrics(sample, "restoreClient");
        log.info("(restoreClient) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(CLIENT_ID.formatted(id)), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CLIENT_BY_ID, key = "#id")
    public ApiResponse<ClientDto> getClientById(UUID id) {
        log.debug("(getClientById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client response = persistenceMethod.getClientById(id);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(CLIENT_ID.formatted(id)));
        }
        recordMetrics(sample, "getClientById");
        log.info("(getClientById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(CLIENT),
                dtoMapper.toClientDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CLIENT_BY_ID, key = "#id")
    public ApiResponse<ClientDto> getDeletedClientById(UUID id) {
        log.debug("(getDeletedClientById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client response = clientRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(CLIENT_ID.formatted(id))));
        recordMetrics(sample, "getDeletedClientById");
        log.info("(getDeletedClientById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(CLIENT),
                dtoMapper.toClientDto(response));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_CLIENTS, key = "#pagedRequestDto")
    @Override
    public ApiResponse<PagedResponse<ClientDto>> listAllActiveClients(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveClients) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        Page<Client> response = clientRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllActiveClients) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllActiveClients");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CLIENTS),
                dtoMapper.toClientDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_CLIENTS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ClientDto>> listAllDeletedClients(PagedRequestDto pagedRequestDto) {
        log.debug("(getAllClientsDeleted) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        Page<Client> response = clientRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(getAllClientsDeleted) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "getAllClientsDeleted");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CLIENTS),
                dtoMapper.toClientDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CLIENTS_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ClientDto>> listClientsByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listClientsByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        Page<Client> response = clientRepository.findByNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listClientsByName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listClientsByName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CLIENTS),
                dtoMapper.toClientDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CLIENTS_DELETED_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ClientDto>> listClientDeletedByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listClientDeletedByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        Page<Client> response = clientRepository.findByNameContainingIgnoreCaseAndActiveFalse(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listClientDeletedByName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listClientDeletedByName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CLIENTS),
                dtoMapper.toClientDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CLIENT_BY_IDENTIFICATION, key = "#identification")
    public ApiResponse<ClientDto> getClientByIdentification(String identification) {
        log.debug("(getClientByIdentification) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client response = persistenceMethod.getClientByIdentification(identification);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(CLIENT_IDENTIFICATION.formatted(identification)));
        }
        recordMetrics(sample, "getClientByIdentification");
        log.info("(getClientByIdentification) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(CLIENT),
                dtoMapper.toClientDto(response));
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("client.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("client.service.operations", "type", operation).increment();
    }
}
