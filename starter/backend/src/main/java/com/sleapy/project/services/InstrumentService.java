package com.sleapy.project.services;

import com.sleapy.project.exceptions.DuplicateInstrumentIDException;
import com.sleapy.project.exceptions.InvalidInstrumentFormatException;
import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.repositories.InstrumentMapper;
import com.sleapy.project.validators.InstrumentValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class InstrumentService {
    private final InstrumentMapper instrumentMapper;
    private final InstrumentValidator instrumentValidator;
    
    public InstrumentService(InstrumentMapper instrumentMapper, InstrumentValidator instrumentValidator) {
        this.instrumentMapper = instrumentMapper;
        this.instrumentValidator = instrumentValidator;
    }

    /**
     * Adds a new instrument to the database after validation
     * @param dto the InstrumentDTO to add
     * @return the saved InstrumentEntity
     * @throws DuplicateInstrumentIDException if symbol already exists
     * @throws InvalidInstrumentFormatException 
     */
    public InstrumentEntity addNewInstrument(InstrumentDTO dto) throws DuplicateInstrumentIDException, InvalidInstrumentFormatException {
        // Validate instrument symbol
        instrumentValidator.validateInstrumentSymbol(dto.getSymbol());

        // Check if instrument with this symbol already exists
        Optional<InstrumentEntity> existingInstrument = instrumentMapper.findBySymbol(dto.getSymbol());
        if (existingInstrument.isPresent()) {
            throw new DuplicateInstrumentIDException(
                "Cannot add the instrument because it already exists in the database: " + dto.getSymbol()
            );
        }

        // Create and save the new instrument entity
        InstrumentEntity entity = new InstrumentEntity(
            null,
            dto.getSymbol(),
            dto.getSymbolName(),
            dto.getInstrumentType()
        );

        instrumentMapper.save(entity);
        return entity;
    }

    /**
     * Retrieves an instrument by ID
     * @param id the instrument ID
     * @return the InstrumentDTO
     * @throws NoSuchElementException if instrument not found
     */
    public InstrumentDTO getInstrumentById(Long id) {
        InstrumentEntity entity = instrumentMapper.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Could not find Instrument by id of " + id
            ));
        return new InstrumentDTO(entity);
    }

    /**
     * Retrieves all instruments
     * @return list of all InstrumentEntity objects
     */
    public List<InstrumentEntity> getAllInstruments() {
        return instrumentMapper.findAll();
    }

    /**
     * Deletes an instrument by ID
     * @param id the instrument ID
     */
    public void deleteInstrument(Long id) {
        InstrumentEntity entity = instrumentMapper.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Could not find Instrument by id of " + id
            ));
        instrumentMapper.deleteById(id);
    }
}