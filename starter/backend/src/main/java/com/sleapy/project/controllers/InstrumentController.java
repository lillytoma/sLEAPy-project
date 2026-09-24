package com.sleapy.project.controllers;

import com.sleapy.project.exceptions.DuplicateInstrumentIDException;
import com.sleapy.project.exceptions.InvalidInstrumentFormatException;
import com.sleapy.project.models.dtos.InstrumentDTO;
import com.sleapy.project.models.entities.InstrumentEntity;
import com.sleapy.project.services.InstrumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {
    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @PostMapping
    public ResponseEntity<InstrumentEntity> addInstrument(@RequestBody InstrumentDTO dto) throws DuplicateInstrumentIDException, InvalidInstrumentFormatException {
        InstrumentEntity instrument = instrumentService.addNewInstrument(dto);
        return new ResponseEntity<>(instrument, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstrumentDTO> getInstrumentById(@PathVariable Long id) {
        InstrumentDTO instrument = instrumentService.getInstrumentById(id);
        return new ResponseEntity<>(instrument, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<InstrumentEntity>> getAllInstruments() {
        List<InstrumentEntity> instruments = instrumentService.getAllInstruments();
        return new ResponseEntity<>(instruments, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstrument(@PathVariable Long id) {
        instrumentService.deleteInstrument(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}