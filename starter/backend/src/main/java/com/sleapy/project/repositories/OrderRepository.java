package com.sleapy.project.repositories;

import com.sleapy.project.models.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByClient_IdOrderByTimeOfPurchaseAsc(Long clientId);

}
