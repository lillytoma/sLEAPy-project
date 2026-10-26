package com.sleapy.project.controllers;



import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sleapy.project.config.APIRouting;
import com.sleapy.project.services.ClientService;
import com.sleapy.project.services.JwtService;
import com.sleapy.project.models.dtos.AddCashRequestDTO;

import java.util.Map;
import java.util.NoSuchElementException;


@RestController
@RequestMapping(APIRouting.CLIENTS_ENDPOINT)
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {
    private final ClientService clientService;
    private final JwtService jwtService;

    ClientController(ClientService cs, JwtService jwtService){
        this.clientService = cs;
        this.jwtService = jwtService;
    }

    @GetMapping("/{clientId}/balance")
    public ResponseEntity<?> getCashBalance(
            @PathVariable Long clientId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        ResponseEntity<?> unauthorized = verifyAuthorization(authorizationHeader);
        if (unauthorized != null) {
            return unauthorized;
        }

        double balance = clientService.getCashBalance(clientId);
        return new ResponseEntity<>(balance, HttpStatus.OK);
    }

    @PostMapping("/{clientId}/deposit")
    public ResponseEntity<?> addCash(@PathVariable Long clientId,
                                     @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                     @RequestBody AddCashRequestDTO request) {
        ResponseEntity<?> unauthorized = verifyAuthorization(authorizationHeader);
        if (unauthorized != null) {
            return unauthorized;
        }

        try {
            return ResponseEntity.ok(clientService.addCash(clientId, request.getAmount()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{clientId}/withdraw")
    public ResponseEntity<?> withdrawCash(@PathVariable Long clientId,
                                         @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                         @RequestBody AddCashRequestDTO request) {
        ResponseEntity<?> unauthorized = verifyAuthorization(authorizationHeader);
        if (unauthorized != null) {
            return unauthorized;
        }

        try {
            return ResponseEntity.ok(clientService.withdrawCash(clientId, request.getAmount()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{clientId}/account-status")
    public ResponseEntity<?> getAccountStatus(@PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(Map.of("frozen", clientService.isFrozen(clientId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{clientId}/freeze")
    public ResponseEntity<?> freezeAccount(@PathVariable Long clientId) {
        try {
            clientService.freeze(clientId);
            return ResponseEntity.ok(Map.of("frozen", true));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{clientId}/unfreeze")
    public ResponseEntity<?> unfreezeAccount(@PathVariable Long clientId) {
        try {
            clientService.unfreeze(clientId);
            return ResponseEntity.ok(Map.of("frozen", false));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long clientId) {
        try {
            clientService.deleteAccount(clientId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> verifyAuthorization(String authorizationHeader) {
        if (jwtService.verifyAuthorizationHeader(authorizationHeader)) {
            return null;
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(java.util.Map.of("error", "Invalid or missing JWT token"));
    }
  

   
}
