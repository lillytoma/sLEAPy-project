package com.sleapy.project.services;

import com.sleapy.project.exceptions.DuplicateInstrumentIDException;
import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.repositories.InstrumentRepository;
import com.sleapy.project.validators.InstrumentValidator;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
//The method assumes that the instrument ID passed is valid and exists in the 
//database (Validation must happen in the validation layer). If the value does exist, 
//the service should call upon the repo and add a new entry into the database. 
//The service should throw a custom error if the instrument ID already exists:
//throw new DuplicateInstrumentIDException("Cannot add the id because it already exists in the database: ", dto.getSymbol());
@Service
public class InstrumentService {
    private final InstrumentRepository instrumentRepository;
    private final InstrumentValidator instrumentValidator;
    
    public InstrumentService(InstrumentRepository instrumentRepository, InstrumentValidator instrumentValidator) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentValidator = instrumentValidator;
    }

    public InstrumentEntity addNewInstrument(InstrumentDTO dto){

         // Check if instrument with this symbol already exists
/*         if (instrumentRepository.existsBySymbol(dto.getSymbol())) {
            throw new DuplicateInstrumentIDException(
                "Cannot add the instrument because it already exists in the database: " + dto.getSymbol()
            );
        } */

        // Create and save the new instrument entity
        InstrumentEntity entity = new InstrumentEntity(
            null,
            dto.getSymbol(),
            dto.getSymbolName(),
            dto.getInstrumentType()
        );

        return instrumentRepository.save(entity);
    }

}
