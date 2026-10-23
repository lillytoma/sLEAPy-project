package com.sleapy.project.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalystClientDTO {
    private Long id;
    private String username;
    private String email;
}
