package com.sleapy.project.utils;

import com.sleapy.project.models.dtos.HoldingDTO;
import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.dtos.ClientDTO;
import com.sleapy.project.models.entities.HoldingEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO Mapper utility class for converting between HoldingEntity and HoldingDTO.
 * Note: This is separate from the MyBatis HoldingMapper interface.
 * 
 * Note: currentMarketValue and unrealizedGainLoss are calculated in the service layer
 * since they require current instrument price which may not always be needed.
 */
public class HoldingDTOUtil {
    public static HoldingDTO toDTO(HoldingEntity entity) {
        if (entity == null) {
            return null;
        }

        HoldingDTO dto = new HoldingDTO();
        
        dto.setHoldingId(entity.getId());
        dto.setQuantity(entity.getQuantityShares());
        
        // Calculate cost basis: quantity × purchase_price
        BigDecimal costBasis = entity.getQuantityShares().multiply(entity.getPurchasePrice());
        dto.setCostBasis(costBasis);
        
        // Convert the instrument entity to DTO
        if (entity.getInstrument() != null) {
            dto.setInstrument(InstrumentDTOUtil.toDTO(entity.getInstrument()));
        }
        
        // Convert the client entity to DTO
        if (entity.getClient() != null) {
            dto.setClient(ClientDTOUtil.toDTO(entity.getClient()));
        }
        
        return dto;
    }

    public static HoldingEntity toEntity(HoldingDTO dto) {
        if (dto == null) {
            return null;
        }

        HoldingEntity entity = new HoldingEntity();
        
        entity.setId(dto.getHoldingId());
        entity.setQuantityShares(dto.getQuantity());
        
        // Note: We need to extract purchase price from costBasis if it's set
        // costBasis = quantity × purchasePrice, so purchasePrice = costBasis / quantity
        if (dto.getCostBasis() != null && dto.getQuantity() != null && dto.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal purchasePrice = dto.getCostBasis().divide(dto.getQuantity(), 2, RoundingMode.HALF_UP);
            entity.setPurchasePrice(purchasePrice);
        }
        
        // Note: Converting DTOs back to entities requires more context
        // Client and Instrument relationships are typically set through the service layer
        
        return entity;
    }
}
