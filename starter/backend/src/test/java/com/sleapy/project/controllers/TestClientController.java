package com.sleapy.project.controllers;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.sleapy.project.services.ClientService;

@RunWith(SpringRunner.class)
@WebMvcTest(ClientController.class)
public class TestClientController {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;
}
