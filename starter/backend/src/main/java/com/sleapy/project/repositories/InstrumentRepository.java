package com.sleapy.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sleapy.project.models.entities.InstrumentEntity;

import java.util.List;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {
    List<InstrumentEntity> findByInstrument_Id(Long instrument_id);

}
