package com.example.registration;

import lombok.Data;

import java.util.List;

@Data
public class ActivateCustomer {

    private String firstName;
    private String lastName;

    boolean active;
}

