package com.sleapy.project.utils;

import com.sleapy.project.models.dtos.HoldingDTO;
import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.dtos.ClientDTO;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.models.enums.InstrumentType;
import java.math.BigDecimal;

/**
 * DTO Mapper utility class for converting between HoldingEntity and HoldingDTO.
 * Note: This is separate from the MyBatis HoldingMapper interface.
 * 
 * Note: currentMarketValue and unrealizedGainLoss are calculated in the service layer
 * since they require current instrument price which may not always be needed.
 */
public class HoldingDTOMapper {
    public static HoldingDTO toDTO(HoldingEntity entity) {
        if (entity == null) {
            return null;
        }

        HoldingDTO dto = new HoldingDTO();
        
        dto.setId(entity.getId());
        dto.setTotalShares(entity.getQuantityShares());
        
        // Calculate total price (cost basis): quantity × purchase_price
        BigDecimal totalPrice = entity.getQuantityShares().multiply(entity.getPurchasePrice());
        dto.setTotalPrice(totalPrice);
        
        // Convert the instrument entity to DTO
        if (entity.getInstrument() != null) {
            com.sleapy.project.models.entities.InstrumentEntity instEntity = entity.getInstrument();
            dto.setInstrument(new InstrumentDTO(
                instEntity.getId(),
                instEntity.getSymbol(),
                instEntity.getSymbolName(),
                InstrumentType.valueOf(instEntity.getInstrumentType().getName()),
                instEntity.getCurrentPrice()
            ));
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
        
        entity.setId(dto.getId());
        entity.setQuantityShares(dto.getTotalShares());
        
        // Note: We need to extract purchase price from totalPrice if it's set
        // totalPrice = quantity × purchasePrice, so purchasePrice = totalPrice / quantity
        if (dto.getTotalPrice() != null && dto.getTotalShares() != null && dto.getTotalShares().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal purchasePrice = dto.getTotalPrice().divide(dto.getTotalShares(), 2, java.math.RoundingMode.HALF_UP);
            entity.setPurchasePrice(purchasePrice);
        }
        
        // Note: Converting DTOs back to entities requires more context
        // Client and Instrument relationships are typically set through the service layer
        
        return entity;
    }
}
