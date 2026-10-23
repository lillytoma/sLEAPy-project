package com.sleapy.project.mappers;

import com.sleapy.project.models.dtos.AnalystPortfolioRowDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnalystPortfolioMapper {
    int capture(@Param("timestamp") LocalDateTime timestamp);

    List<AnalystPortfolioRowDTO> findSnapshotForClients(@Param("clientIds") List<Long> clientIds,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
            @Param("latest") boolean latest);

    LocalDateTime findHistorySinceForClients(@Param("clientIds") List<Long> clientIds);

    default List<AnalystPortfolioRowDTO> findSnapshot(Long clientId, LocalDateTime start, LocalDateTime end, boolean latest) {
        return findSnapshotForClients(clientId == null ? List.of() : List.of(clientId), start, end, latest);
    }

    default LocalDateTime findHistorySince(Long clientId) {
        return findHistorySinceForClients(clientId == null ? List.of() : List.of(clientId));
    }
}
