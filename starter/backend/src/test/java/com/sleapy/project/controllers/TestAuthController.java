// package com.sleapy.project.controllers;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// import org.springframework.http.MediaType;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.web.servlet.MockMvc;
// import com.sleapy.project.models.LoginRequestDTO;
// import com.sleapy.project.services.ClientService;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
// @WebMvcTest(AuthController.class) // Test only the web layer (controller)
// class TestAuthController {
 
//     @Autowired
//     private MockMvc mockMvc; // Simulates HTTP requests
 
//     @MockitoBean // Mocks the controller dependency used by the web layer
//     private ClientService clientService;
 
//     // Test cases will go here
//     @Test
//     void loginReturnsOKForValidCredentials() throws Exception {
//         // Arrange
//         LoginRequestDTO request = new LoginRequestDTO(
//             "Test@example.com",
//             "TestPassword123"
//         );

//         when(clientService.checkCredentials(anyString(), anyString())).thenReturn(true);

//         mockMvc.perform(post("/api/auth/login")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content("{\"email\":\"" + request.getEmail() + "\",\"password\":\"" + request.getPassword() + "\"}"))
//                 .andExpect(status().isOk())
//                 .andExpect(content().string("Login successful"));
//     }

//     // @Test
//     // void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
//     //     //Arrange
//     //     LoginRequestDTO request = new LoginRequestDTO(
//     //         "Test@example.com",
//     //         "WrongPassword123"
//     //     );
//     //     when(clientService.checkCredentials(anyString(), anyString())).thenReturn(false);

//     //     mockMvc.perform(post("/api/auth/login")
//     //                     .contentType(MediaType.APPLICATION_JSON)
//     //                     .content("{\"email\":\"" + request.getEmail() + "\", \"password\":\"" + request.getPassword() + "\"}"))
//     //             .andExpect(status().isUnauthorized())
//     //             .andExpect(content().string("Login Unauthroized"));
    
//     // }
// }