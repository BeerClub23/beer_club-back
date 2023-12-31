package com.digitalHouse.beerClub.controller;

import com.digitalHouse.beerClub.exceptions.NotFoundException;
import com.digitalHouse.beerClub.model.dto.AddressDTO;
import com.digitalHouse.beerClub.service.interfaces.IAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Address")
@RequestMapping("/address")
@Validated
public class AddressController {

    @Autowired
    private IAddressService addressService;

    @Operation(summary ="Find address by user Email", description ="Find address by user Email", responses = {
        @ApiResponse(responseCode = "200",description = "OK",content = @Content(mediaType = "application/json",schema = @Schema(implementation = AddressDTO.class))),
        @ApiResponse(responseCode = "404", description = "NOT_FOUND", content = @Content(mediaType = "text/plain", schema = @Schema(defaultValue= "Address not found with userEmail: example@beerClub")))})
    @GetMapping("/{userEmail}")
    public ResponseEntity<AddressDTO> getAddressByUserEmail(@PathVariable String userEmail) throws NotFoundException {
        AddressDTO address = addressService.getAddressByUserAuth(userEmail);
        return ResponseEntity.ok(address);
    }

    @Operation(summary ="Update address by user Email", description ="Update address by user Email", responses = {
        @ApiResponse(responseCode = "200",description = "OK",content = @Content(mediaType = "application/json",schema = @Schema(implementation = AddressDTO.class))),
        @ApiResponse(responseCode = "404", description = "NOT_FOUND", content = @Content(mediaType = "text/plain", schema = @Schema(defaultValue= "Address not found with userEmail: example@beerClub")))})
    @PutMapping("/update")
    public ResponseEntity<AddressDTO> updateAddressByUserEmail(Authentication authentication, @Valid @RequestBody AddressDTO updatedAddress) throws NotFoundException {
        AddressDTO updated = addressService.updateAddressByUserEmail(authentication, updatedAddress);
        return ResponseEntity.ok(updated);
    }

}

