package com.digitalHouse.beerClub.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private Long id;
    private  Long product_id;
    private  Long user_id;
    private Integer rating;
    private String comments;

}