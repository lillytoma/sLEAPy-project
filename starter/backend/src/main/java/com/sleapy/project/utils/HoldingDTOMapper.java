package com.sleapy.project.utils;

import com.sleapy.project.models.dtos.HoldingDTO;
import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.dtos.ClientDTO;
import com.sleapy.project.models.entities.HoldingEntity;

/**
 * DTO Mapper utility class for converting between HoldingEntity and HoldingDTO.
 * Note: This is separate from the MyBatis HoldingMapper interface.
 */
public class HoldingDTOMapper {
    public static HoldingDTO toDTO(HoldingEntity entity) {
        if (entity == null) {
            return null;
        }

        HoldingDTO dto = new HoldingDTO();
        
        dto.setHoldingId(entity.getId());
        dto.setTotalShares(entity.getQuantityShares().doubleValue());
        dto.setTotalPrice(entity.getPurchasePrice().doubleValue());
        
        // Convert the instrument entity to DTO
        if (entity.getInstrument() != null) {
            dto.setInstrument(new InstrumentDTO(entity.getInstrument()));
        }
        
        // Convert the client entity to DTO
        if (entity.getClient() != null) {
            dto.setClient(ClientDTOMapper.toDTO(entity.getClient()));
        }
        
        return dto;
    }

    public static HoldingEntity toEntity(HoldingDTO dto) {
        if (dto == null) {
            return null;
        }

        HoldingEntity entity = new HoldingEntity();
        
        entity.setId(dto.getHoldingId());
        entity.setQuantityShares(java.math.BigDecimal.valueOf(dto.getTotalShares()));
        entity.setPurchasePrice(java.math.BigDecimal.valueOf(dto.getTotalPrice()));
        
        // Note: Converting DTOs back to entities requires more context
        // This is typically done through the service layer with proper mapper lookups
        
        return entity;
    }
}
