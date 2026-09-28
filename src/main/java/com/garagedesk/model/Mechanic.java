package com.garagedesk.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Mechanic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String name;
    public String specialty;

    public Mechanic() {}
    public Mechanic(String name, String specialty) { this.name = name; this.specialty = specialty; }
}
