package com.sigi.services.service.client;

import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.client.NewClientDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;

import java.util.UUID;

public interface ClientService {
    ApiResponse<ClientDto> createClient(NewClientDto dto);

    ApiResponse<ClientDto> updateClient(UUID id, NewClientDto dto);

    ApiResponse<Void> deleteClient(UUID id);

    ApiResponse<Void> restoreClient(UUID id);

    ApiResponse<ClientDto> getClientById(UUID id);

    ApiResponse<ClientDto> getDeletedClientById(UUID id);

    ApiResponse<PagedResponse<ClientDto>> listAllActiveClients(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ClientDto>> listAllDeletedClients(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ClientDto>> listClientsByName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ClientDto>> listClientDeletedByName(PagedRequestDto pagedRequestDto);

    ApiResponse<ClientDto> getClientByIdentification(String identification);
}
