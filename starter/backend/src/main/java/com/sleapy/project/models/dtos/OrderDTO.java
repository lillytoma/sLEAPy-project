package com.sleapy.project.models.dtos;

import java.time.LocalDate;

import com.sleapy.project.models.entities.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class OrderDTO{

    @NotNull(message = "order id is required")
    private Long id;

    @NotNull(message = "time of purchase is required")
    @PastOrPresent(message = "time of purchase cannot be in the future")
    private LocalDate timeOfPurchase;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private int quantity;

    @PastOrPresent(message = "time filled cannot be in the future")
    private LocalDate timeFilled;

    @NotNull(message = "purchase price is required")
    @PositiveOrZero(message = "price must be non-negative")
    private double purchasePrice;

    @NotNull(message = "instrument is required")
    @Valid
    private InstrumentDTO instrument;

    @NotNull(message = "order status is required")
    private OrderStatus status;
}
