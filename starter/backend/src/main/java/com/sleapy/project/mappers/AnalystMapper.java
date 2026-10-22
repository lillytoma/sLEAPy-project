package com.sleapy.project.mappers;

import com.sleapy.project.models.dtos.AnalystClientDTO;
import com.sleapy.project.models.dtos.AnalystInstrumentActivityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AnalystMapper {
    BigDecimal findTradeVolume(@Param("clientId") Long clientId);

    long countTrades(@Param("clientId") Long clientId);

    String findLatestTradeTime(@Param("clientId") Long clientId);

    List<AnalystInstrumentActivityDTO> findMostActiveInstruments(@Param("clientId") Long clientId);

    List<AnalystClientDTO> searchClients(@Param("search") String search);
}
