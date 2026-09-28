package com.sleapy.project.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sleapy.project.config.APIRouting;
import com.sleapy.project.exceptions.InvalidEmailFormatException;
import com.sleapy.project.models.dtos.LoginRequestDTO;
import com.sleapy.project.models.dtos.SignUpRequestDTO;
import com.sleapy.project.models.dtos.SignUpResponseDTO;
import com.sleapy.project.models.entities.ClientEntity;
import com.sleapy.project.services.ClientService;
import com.sleapy.project.services.JwtService;
import com.sleapy.project.validators.ClientValidator;

import lombok.AllArgsConstructor;


@RestController
@RequestMapping(APIRouting.AUTH_ENDPOINT)
@AllArgsConstructor
@CrossOrigin("http://localhost:4200")
public class AuthController {
    //The ClientService is injected into the AuthController to handle authentication logic, 
    // such as checking user credentials against the database.
    private final ClientService clientService;
    private final ClientValidator clientValidator;
    private final JwtService jwtService;


    //The login method handles POST requests to the /login endpoint. It takes a LoginRequestDTO object containing the user's email and password, checks the credentials using the ClientService, and returns a ResponseEntity indicating whether the login was successful or not.
    @PostMapping("/login")
    //The login method handles POST requests to the /login endpoint.
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO request) {
        //The login method handles POST requests to the /login endpoint. It takes a LoginRequestDTO object containing the user's email and password, checks the credentials using the ClientService, and returns a ResponseEntity indicating whether the login was successful or not.
        try {
            clientValidator.validateEmail(request.getEmail());
        }catch(InvalidEmailFormatException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
        boolean isValid = clientService.checkCredentials(request.getEmail(), request.getPassword());
        //The login method handles POST requests to the /login endpoint.
        if (isValid) {
            return new ResponseEntity<>("Login successful", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid email or password\n", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequestDTO request) {
        try {
            // Step 1: Validate email format
            clientValidator.validateEmail(request.getEmail());
            
            // Step 2: Check if email already exists
            if (clientService.isUniqueEmail(request.getEmail())) {
                return new ResponseEntity<>(
                    "Email already registered", 
                    HttpStatus.CONFLICT
                );
            }
            
            // Step 3: Create new client in database
            ClientEntity newClient = clientService.signup(request);
            
            // Step 4: Generate JWT token
            String jwtToken = jwtService.generateToken(newClient.getEmail(), newClient.getId());
            
            // Step 5: Build response
            SignUpResponseDTO response = new SignUpResponseDTO(
                "Signup successful. Welcome!",
                newClient.getId(),
                jwtToken
            );
            
            // Step 6: Return 201 CREATED (RESTful standard)
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (InvalidEmailFormatException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>(
                "Signup failed: " + ex.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
