package com.sleapy.project.utils;

import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.entities.InstrumentEntity;

/**
 * DTO Mapper utility class for converting between InstrumentEntity and InstrumentDTO.
 * Note: This is separate from the MyBatis InstrumentMapper interface.
 */
public class InstrumentDTOUtil {
    
    public static InstrumentDTO toDTO(InstrumentEntity entity) {
        if (entity == null) {
            return null;
        }

        InstrumentDTO dto = new InstrumentDTO();
        
        dto.setSymbol(entity.getSymbol());
        dto.setSymbolName(entity.getSymbolName());
        dto.setInstrumentType(entity.getInstrumentType());
        dto.setCurrentPrice(entity.getCurrentPrice());
        
        return dto;
    }

    public static InstrumentEntity toEntity(InstrumentDTO dto) {
        if (dto == null) {
            return null;
        }

        InstrumentEntity entity = new InstrumentEntity();
        
        entity.setSymbol(dto.getSymbol());
        entity.setSymbolName(dto.getSymbolName());
        entity.setInstrumentType(dto.getInstrumentType());
        entity.setCurrentPrice(dto.getCurrentPrice());
        
        return entity;
    }
}
