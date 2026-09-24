package com.sleapy.project.utils;
import com.sleapy.project.models.dtos.ClientDTO;
import com.sleapy.project.models.entities.ClientEntity;

/**
 * DTO Mapper utility class for converting between ClientEntity and ClientDTO.
 * Note: This is separate from the MyBatis ClientMapper interface.
 */
public class ClientDTOMapper {
    public static ClientDTO toDTO(ClientEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientDTO dto = new ClientDTO();

        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        //dto.setName(entity.getName());
        dto.setCashBalance(entity.getCashBalance());

        return dto;
    }

    public static ClientEntity toEntity(ClientDTO dto) {
        if (dto == null) {
            return null;
        }

        ClientEntity entity = new ClientEntity();

        entity.setId(dto.getId());
        entity.setEmail(dto.getEmail());
        //entity.setName(dto.getName());
        entity.setCashBalance(dto.getCashBalance());

        return entity;
    }
}
