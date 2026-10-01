package com.sleapy.project.validators;

import org.springframework.stereotype.Service;

import com.sleapy.project.models.entities.OrderEntity;
import java.time.LocalDate;
import java.math.BigDecimal;

import com.sleapy.project.exceptions.InvalidOrderException;



@Service 
public class OrderValidator {
	/**
    * Validates all fields and business rules for an order
    * @param order the order to validate
    * @throws InvalidOrderException if validation fails
    */
    public void validateOrder(OrderEntity order) throws InvalidOrderException{

        if(order == null){
            throw new InvalidOrderException("Order cannot be null");
        }

        validateQuantity(order.getQuantity());

        validatePurchasePrice(order.getPurchasePrice());

        validatePurchaseDate(order.getTimeOfPurchase());

        validateClient(order.getClient());
        validateInstrument(order.getInstrument());
    }
 
    // Quantity must be positive
    private void validateQuantity(Integer quantity) throws InvalidOrderException{
        if(quantity == null){
            throw new InvalidOrderException("Quantity cannot be null");
        }
        if(quantity <= 0){
            throw new InvalidOrderException("Quantity must be greater than 0. Received: " + quantity);
        }
    }
    
    // Price must be positive and not null
    private void validatePurchasePrice(BigDecimal purchasePrice) throws InvalidOrderException{
        if(purchasePrice == null){
            throw new InvalidOrderException("Purchase price cannot be null");
        }
        if(purchasePrice.compareTo(BigDecimal.ZERO) <= 0){
            throw new InvalidOrderException("Purchase price must be greater than 0. Received: " + purchasePrice);
        }
    }

    // Purchase date must not be in the future
    private void validatePurchaseDate(LocalDate timeOfPurchase) throws InvalidOrderException{
        if(timeOfPurchase == null){
            throw new InvalidOrderException("Purchase date cannot be null");
        }
        if(timeOfPurchase.isAfter(LocalDate.now())){
            throw new InvalidOrderException("Purchase date cannot be in the future. Received: " + timeOfPurchase);
        }
    }

    // Order must reference an existing client
    private void validateClient(Object client) throws InvalidOrderException{
        if(client == null){
            throw new InvalidOrderException("Order must be associated with a client");
        }
    }

    // Order must specify an instrument to buy
    private void validateInstrument(Object instrument) throws InvalidOrderException{
        if(instrument == null){
            throw new InvalidOrderException("Order must specify an instrument");
        }
    }
}
