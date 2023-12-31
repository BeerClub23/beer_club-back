package com.digitalHouse.beerClub.model.dto;

import com.digitalHouse.beerClub.model.Address;
import com.digitalHouse.beerClub.model.RoleType;
import lombok.*;

import java.time.LocalDate;

@Data
public class UserDTO {
    private Long id;
    private String firstName, lastName, email;
    private LocalDate birthdate;
    private String telephone;
    private LocalDate subscriptionDate;
    private Address address;
    private RoleType role;
    private boolean active;
    private Long subscriptionId;
    private Long nextSubscriptionId;
}
