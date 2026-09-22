package com.sleapy.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sleapy.project.models.entities.OrderEntity;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByClient_IdOrderByTimePurchasedAsc(Long clientId);

}
