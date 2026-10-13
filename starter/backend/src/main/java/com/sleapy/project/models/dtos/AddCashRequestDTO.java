package com.sleapy.project.models.dtos;

import java.math.BigDecimal;

import lombok.Data;


@Data 
public class AddCashRequestDTO {
    private BigDecimal amount;
}
