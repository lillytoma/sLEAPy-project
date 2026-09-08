package com.sleapy.project.util;
import com.sleapy.project.models.ClientDTO;
import com.sleapy.project.models.ClientEntity;

public class ClientMapper {
    public static ClientDTO toDTO(ClientEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientDTO dto = new ClientDTO();

        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setName(entity.getName());
        dto.setCurrentBalance(entity.getCurrentBalance());

        return dto;
    }

    public static ClientEntity toEntity(ClientDTO dto) {
        if (dto == null) {
            return null;
        }

        ClientEntity entity = new ClientEntity();

        entity.setId(dto.getId());
        entity.setEmail(dto.getEmail());
        entity.setName(dto.getName());
        entity.setCurrentBalance(dto.getCurrentBalance());

        return entity;
    }
}
