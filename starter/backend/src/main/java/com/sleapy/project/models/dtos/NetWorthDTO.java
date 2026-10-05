package com.sleapy.project.models.dtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;


//Data Transfer Object for customer net worth summary.
//Contains aggregated portfolio value, holdings breakdown and cash balance.

@Data
@NoArgsConstructor 
@AllArgsConstructor
public class NetWorthDTO {
    private Long clientId;  //client identifier
    private BigDecimal cashBalance; //available cash in account
    private BigDecimal totalCostBasis; //sum of (quantity x purchase price) for all holdings
    private BigDecimal totalCurrentValue; //sum (quantity x current price) for all holdings
    private BigDecimal unrealizedGainLoss; //total current value - total cost balance
    private BigDecimal totalNetWorth; //total current value + cash balance
    private List<HoldingDTO> holdings; //detailed breakdown of each holding
}
