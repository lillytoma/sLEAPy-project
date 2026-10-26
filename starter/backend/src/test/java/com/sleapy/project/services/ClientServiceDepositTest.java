package com.sleapy.project.services;

import com.sleapy.project.mappers.ClientMapper;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.models.entities.ClientStatus;
import com.sleapy.project.validators.ClientValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceDepositTest {
    @Mock private ClientMapper clientMapper;
    @Mock private ClientValidator clientValidator;
    @InjectMocks private ClientService clientService;

    private ClientEntity client(String statusName) {
        ClientEntity client = new ClientEntity();
        ClientStatus status = new ClientStatus();
        status.setName(statusName);
        client.setClientStatus(status);
        client.setCashBalance(125.50);
        return client;
    }

    @Test
    void frozenAccountsCannotDeposit() {
        when(clientMapper.findById(1L)).thenReturn(Optional.of(client("SuSpEnDeD")));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> clientService.addCash(1L, new BigDecimal("25.50")));

        assertTrue(error.getMessage().contains("frozen"));
        verify(clientMapper, never()).addCash(anyLong(), any());
    }

    @Test
    void activeAccountsCanDepositAfterStatusIsLockedAndChecked() {
        when(clientMapper.findById(1L)).thenReturn(Optional.of(client("Active")));

        assertEquals(125.50, clientService.addCash(1L, new BigDecimal("25.50")));

        var order = inOrder(clientMapper);
        order.verify(clientMapper).lockForTrading(1L);
        order.verify(clientMapper).findById(1L);
        order.verify(clientMapper).addCash(1L, new BigDecimal("25.50"));
    }

    @Test
    void rejectsInvalidAmountsBeforeAccessingAccount() {
        assertThrows(IllegalArgumentException.class, () -> clientService.addCash(1L, null));
        for (String amount : new String[]{"0", "-1", "1.001"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> clientService.addCash(1L, new BigDecimal(amount)));
        }
        verifyNoInteractions(clientMapper);
    }

    @Test
    void missingAccountsCannotDeposit() {
        when(clientMapper.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class,
                () -> clientService.addCash(1L, BigDecimal.ONE));
        verify(clientMapper, never()).addCash(anyLong(), any());
    }
}
