package com.sleapy.project.controllers;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import com.sleapy.project.services.ClientService;
@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(ClientController.class) // Test only the web layer (controller)
public class ClientControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean 
    private ClientService clientService;

    
}
