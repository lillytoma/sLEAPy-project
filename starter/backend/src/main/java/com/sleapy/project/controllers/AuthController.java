package com.sleapy.project.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sleapy.project.config.APIRouting;
import com.sleapy.project.models.dtos.LoginRequestDTO;
import com.sleapy.project.services.ClientService;
import lombok.AllArgsConstructor;


@RestController
@RequestMapping(APIRouting.AUTH_ENDPOINT)
@AllArgsConstructor
@CrossOrigin("http://localhost:4200")
public class AuthController {
    //The ClientService is injected into the AuthController to handle authentication logic, 
    // such as checking user credentials against the database.
    private final ClientService clientService;
    //The login method handles POST requests to the /login endpoint.
    @PostMapping("/login")
    //The login method handles POST requests to the /login endpoint.
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO request) {
        //It takes a LoginRequestDTO object, containing the user's email and password, 
        //checks the credentials using the ClientService, and returns a ResponseEntity 
        //indicating whether the login was successful or not.
        boolean isValid = clientService.checkCredentials(request.getEmail(), request.getPassword());
        //The login method handles POST requests to the /login endpoint.
        if (isValid) {
            return new ResponseEntity<>("Login successful", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid email or password\n", HttpStatus.UNAUTHORIZED);
        }
    }
}
