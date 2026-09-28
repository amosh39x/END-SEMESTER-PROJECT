package com.garagedesk.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Bay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String name;
    public String type;

    public Bay() {}
    public Bay(String name, String type) { this.name = name; this.type = type; }
}
