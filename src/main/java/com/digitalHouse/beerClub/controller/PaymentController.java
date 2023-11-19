package com.digitalHouse.beerClub.controller;

import com.digitalHouse.beerClub.auth.UserAuthRequest;
import com.digitalHouse.beerClub.exceptions.NotFoundException;
import com.digitalHouse.beerClub.model.PaymentStatus;
import com.digitalHouse.beerClub.model.dto.PaymentDTO;
import com.digitalHouse.beerClub.model.dto.UserDTO;
import com.digitalHouse.beerClub.service.interfaces.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Payments")
@RequestMapping("/payments")
@Validated
public class PaymentController {

    @Autowired
    private IPaymentService paymentService;

    @Operation(summary="List all payments", description="List all payments", responses = {
        @ApiResponse(responseCode = "200",description = "OK",content = @Content(mediaType = "application/json",array = @ArraySchema(schema = @Schema(implementation = PaymentDTO.class))))})
    @GetMapping("/all")
    public ResponseEntity<List<PaymentDTO>> getPayments() {
        List<PaymentDTO> paymentDTOS = paymentService.searchAll();
        return new ResponseEntity<>(paymentDTOS, HttpStatus.OK);
    }

    @Operation(summary="List all payments for a user", description="List all payments for a user", responses = {
        @ApiResponse(responseCode = "200",description = "OK",content = @Content(mediaType = "application/json",array = @ArraySchema(schema = @Schema(implementation = PaymentDTO.class))))})
    @GetMapping("/paymentUser/{userId}")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByUserId(@PathVariable Long userId) {
        List<PaymentDTO> paymentDTOS = paymentService.findPaymentsByUserId(userId);
        return new ResponseEntity<>(paymentDTOS, HttpStatus.OK);
    }

    @Operation(summary ="Find payment by ID", description ="Find payment by ID", responses = {
        @ApiResponse(responseCode = "200",description = "OK",content = @Content(mediaType = "application/json",schema = @Schema(implementation = PaymentDTO.class))),
        @ApiResponse(responseCode = "404", description = "NOT_FOUND", content = @Content(mediaType = "text/plain", schema = @Schema(defaultValue= "Payment not found with ID: 1")))})
    @GetMapping("/id/{id}")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable @Positive(message = "Id must be greater than 0") Long id) throws NotFoundException {
        PaymentDTO paymentDTO = paymentService.searchById(id);
        return new ResponseEntity<>(paymentDTO, HttpStatus.OK);
    }

    @Operation(summary = "Update the payment status", description = "Update the status of an existing payment",responses = {
        @ApiResponse(responseCode = "200",description = "OK",content = @Content(mediaType = "text/plain",schema = @Schema(implementation = PaymentDTO.class))),
    @ApiResponse(responseCode = "404", description = "NOT_FOUND", content = @Content(mediaType = "text/plain", schema = @Schema(defaultValue= "Payment not found")))})
    @PatchMapping("/status/{paymentId}")
    public ResponseEntity<PaymentDTO> updatePaymentStatus(@PathVariable Long paymentId, @RequestParam PaymentStatus newStatus) throws NotFoundException {
        PaymentDTO paymentDTO = paymentService.updatePaymentStatus(paymentId, newStatus);
        return new ResponseEntity<>(paymentDTO, HttpStatus.OK);
    }
}
