package com.sleapy.project.services;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import com.sleapy.project.mappers.HoldingMapper;
import com.sleapy.project.models.dtos.HoldingDTO;
import com.sleapy.project.models.entities.HoldingEntity;
import com.sleapy.project.utils.HoldingDTOMapper;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CrossOrigin(origins = "http://localhost:4200")
public class HoldingsService {
    private final HoldingMapper holdingMapper;

    public HoldingsService(HoldingMapper holdingMapper) {
        this.holdingMapper = holdingMapper;
    }

    /**
     * Retrieve all holdings for a specific client.
     * @param clientId the ID of the client
     * @return List of HoldingDTO objects for the client
     */
    public List<HoldingDTO> getClientHoldings(Long clientId) {
        List<HoldingEntity> holdings = holdingMapper.findByClientId(clientId);
        return holdings.stream()
                .map(HoldingDTOMapper::toDTO)
                .collect(Collectors.toList());
    }
}
