package com.sleapy.project.services;

import com.sleapy.project.exceptions.InsufficientCashException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceWithdrawalTest {
    @Mock private ClientMapper clientMapper;
    @Mock private ClientValidator clientValidator;
    @InjectMocks private ClientService clientService;

    private ClientEntity client(String statusName, double balance) {
        var client = new ClientEntity();
        var status = new ClientStatus();
        status.setName(statusName);
        client.setClientStatus(status);
        client.setCashBalance(balance);
        return client;
    }

    @Test
    void withdrawalLocksAccountAndReturnsPersistedBalance() {
        when(clientMapper.findById(1L)).thenReturn(
                Optional.of(client("Active", 125.50)), Optional.of(client("Active", 100)));
        when(clientMapper.withdrawCash(1L, new BigDecimal("25.50"))).thenReturn(1);

        assertEquals(100, clientService.withdrawCash(1L, new BigDecimal("25.50")));

        var order = inOrder(clientMapper);
        order.verify(clientMapper).lockForTrading(1L);
        order.verify(clientMapper).findById(1L);
        order.verify(clientMapper).withdrawCash(1L, new BigDecimal("25.50"));
        order.verify(clientMapper).findById(1L);
    }

    @Test
    void frozenAccountsCannotWithdraw() {
        when(clientMapper.findById(1L)).thenReturn(Optional.of(client("SuSpEnDeD", 125.50)));
        var error = assertThrows(IllegalStateException.class,
                () -> clientService.withdrawCash(1L, BigDecimal.ONE));
        assertTrue(error.getMessage().contains("frozen"));
        verify(clientMapper, never()).withdrawCash(anyLong(), any());
    }

    @Test
    void insufficientBalanceDoesNotReturnSuccess() {
        when(clientMapper.findById(1L)).thenReturn(Optional.of(client("Active", 125.50)));
        assertThrows(InsufficientCashException.class,
                () -> clientService.withdrawCash(1L, new BigDecimal("125.51")));
        verify(clientMapper, times(1)).findById(1L);
    }

    @Test
    void rejectsInvalidAmountsBeforeAccessingAccount() {
        assertThrows(IllegalArgumentException.class, () -> clientService.withdrawCash(1L, null));
        for (String amount : new String[]{"0", "-1", "1.001"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> clientService.withdrawCash(1L, new BigDecimal(amount)));
        }
        verifyNoInteractions(clientMapper);
    }

    @Test
    void missingAccountsCannotWithdraw() {
        when(clientMapper.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> clientService.withdrawCash(1L, BigDecimal.ONE));
        verify(clientMapper, never()).withdrawCash(anyLong(), any());
    }
}
