package com.sleapy.project.mappers;

import com.sleapy.project.models.dtos.AnalystClientDTO;
import com.sleapy.project.models.dtos.AnalystActivityDTO;
import com.sleapy.project.models.dtos.AnalystInstrumentActivityDTO;
import com.sleapy.project.models.dtos.AnalystOrderDirectionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnalystMapper {
    List<AnalystOrderDirectionDTO> findOrderDirectionsForClients(@Param("clientIds") List<Long> clientIds,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<AnalystActivityDTO> findActivityForClients(@Param("clientIds") List<Long> clientIds,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    BigDecimal findTradeVolumeForClients(@Param("clientIds") List<Long> clientIds,
                              @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countTradesForClients(@Param("clientIds") List<Long> clientIds,
                     @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    String findLatestTradeTimeForClients(@Param("clientIds") List<Long> clientIds,
                              @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<AnalystInstrumentActivityDTO> findMostActiveInstrumentsForClients(@Param("clientIds") List<Long> clientIds,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<AnalystClientDTO> searchClients(@Param("search") String search);

    default List<AnalystOrderDirectionDTO> findOrderDirections(Long clientId, LocalDateTime start, LocalDateTime end) {
        return findOrderDirectionsForClients(clientId == null ? List.of() : List.of(clientId), start, end);
    }

    default List<AnalystActivityDTO> findActivity(Long clientId, LocalDateTime start, LocalDateTime end) {
        return findActivityForClients(clientId == null ? List.of() : List.of(clientId), start, end);
    }

    default BigDecimal findTradeVolume(Long clientId, LocalDateTime start, LocalDateTime end) {
        return findTradeVolumeForClients(clientId == null ? List.of() : List.of(clientId), start, end);
    }

    default long countTrades(Long clientId, LocalDateTime start, LocalDateTime end) {
        return countTradesForClients(clientId == null ? List.of() : List.of(clientId), start, end);
    }

    default String findLatestTradeTime(Long clientId, LocalDateTime start, LocalDateTime end) {
        return findLatestTradeTimeForClients(clientId == null ? List.of() : List.of(clientId), start, end);
    }

    default List<AnalystInstrumentActivityDTO> findMostActiveInstruments(Long clientId, LocalDateTime start, LocalDateTime end) {
        return findMostActiveInstrumentsForClients(clientId == null ? List.of() : List.of(clientId), start, end);
    }
}
