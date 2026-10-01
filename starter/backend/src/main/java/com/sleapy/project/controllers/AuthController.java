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
import com.sleapy.project.mappers.ClientMapper;
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
    private final ClientMapper clientMapper;
    private final JwtService jwtService;


    //The login method handles POST requests to the /login endpoint. It takes a LoginRequestDTO object containing the user's email and password, checks the credentials using the ClientService, and returns a ResponseEntity indicating whether the login was successful or not.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            // Step 1: Validate email format
            clientValidator.validateEmail(request.getEmail());

            //2. check if the credentials are valid
            boolean isValid = clientService.checkCredentials(request.getEmail(), request.getPassword());
            
            //3. if the email / password are invalid throw a failed repsonse
            if (!isValid) {
                return new ResponseEntity<>(
                    "Invalid email or password", 
                    HttpStatus.UNAUTHORIZED
                );
            }

            //4. if credentials are valid then reach out to the mapper to reach the db
            ClientEntity user = clientMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Step 4: Generate JWT token
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getId());

            // Step 5: Build response
            SignUpResponseDTO response = new SignUpResponseDTO(
                "Login successful",
                user.getId(),
                jwtToken
            );

            //6. return the 200 status OK
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (InvalidEmailFormatException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            ex.printStackTrace();
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            return new ResponseEntity<>(
                "Login failed: " + errorMsg, 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
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
            ex.printStackTrace();
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            return new ResponseEntity<>(
                "Signup failed: " + errorMsg, 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
