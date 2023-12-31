
package com.digitalHouse.beerClub.service.implement;

import com.digitalHouse.beerClub.config.MyAppConfig;
import com.digitalHouse.beerClub.exceptions.*;
import com.digitalHouse.beerClub.mapper.Mapper;
import com.digitalHouse.beerClub.model.*;
import com.digitalHouse.beerClub.model.dto.*;
import com.digitalHouse.beerClub.repository.IPaymentRepository;
import com.digitalHouse.beerClub.repository.ISubscriptionRepository;
import com.digitalHouse.beerClub.repository.IUserRepository;
import com.digitalHouse.beerClub.service.interfaces.IPaymentService;
import com.digitalHouse.beerClub.utils.AccountUtils;
import com.digitalHouse.beerClub.utils.CardUtils;
import com.digitalHouse.beerClub.utils.TransformationUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImplement implements IPaymentService {

    private final IPaymentRepository paymentRepository;
    private final ISubscriptionRepository subscriptionRepository;
    private final CardService cardService;
    private final AccountService accountService;
    private final Mapper paymentMapper;
    private final MyAppConfig myAppConfig;
    private final IUserRepository userRepository;

    @Autowired
    public PaymentServiceImplement(IPaymentRepository paymentRepository, ISubscriptionRepository subscriptionRepository, CardService cardService, AccountService accountService, Mapper mapper, MyAppConfig myAppConfig, IUserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.cardService = cardService;
        this.accountService = accountService;
        this.paymentMapper = mapper;
        this.myAppConfig = myAppConfig;
        this.userRepository = userRepository;
    }

    // Método para que el administrador pueda acceder a todos los pagos.
    @Override
    public List<PaymentDTO> searchAll() {
        return paymentRepository.findAll().stream().map(payment -> paymentMapper.converter(payment, PaymentDTO.class)).toList();
    }

    // Método para que el administrador pueda acceder a los pagos de un usuario por su id.
    @Override
    public List<PaymentDTO> findPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream().map(payment -> paymentMapper.converter(payment, PaymentDTO.class)).toList();
    }

    // Método para que el usuario autenticado pueda acceder a su pago por id.
    @Override
    public PaymentDTO searchById(Long id, Authentication authentication) throws NotFoundException, ForbiddenException {
        User user = userRepository.findByEmail(authentication.getName());

        if (!user.isActive()) {
            throw new ForbiddenException("El usuario no está activo");
        }
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new NotFoundException("No se encontró el pago con ID: " + id));
        //return paymentMapper.converter(payment, PaymentDTO.class);
        if (payment.getUserId().equals(user.getId())) {
            return paymentMapper.converter(payment, PaymentDTO.class);
        } else {
            throw new ForbiddenException("El pago no pertenece al usuario autenticado");
        }
    }

    // Método para que el usuario autenticado pueda acceder a todos sus pagos.
    @Override
    public List<PaymentDTO> getPaymentsUserAuth(Authentication authentication) throws NotFoundException, ForbiddenException {
        User user =  userRepository.findByEmail(authentication.getName());

        if (!user.isActive()) {
            throw new ForbiddenException("El usuario no está activo");
        }

        List<Payment> userPayments = paymentRepository.findByUserId(user.getId());

        if (userPayments.isEmpty()) {
            throw new NotFoundException("No se encontraron pagos para el usuario con ID: " + user.getId());
        }

        return userPayments.stream()
                .map(payment -> paymentMapper.converter(payment, PaymentDTO.class))
                .toList();
    }

    // Método para crear un pago cuando el usuario se registra por 1º vez.
    @Transactional
    @Override
    public Payment savePayment(CardPayment card, User user) throws NotFoundException, InsufficientBalanceException {
        //Long accountBeerClubId = 1L;
        String accountBeerClubNumber = myAppConfig.getAccountNumber();
        Subscription subscription = subscriptionRepository.findById(user.getSubscription().getId()).orElseThrow(() -> new NotFoundException("No se encontró la suscripción"));

        Double amount = subscription.getPrice();
        String description = subscription.getName();
        String invoiceNumber = AccountUtils.getInvoiceNumber();

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setDescription(description);
        payment.setDate(LocalDateTime.now());
        payment.setCardNumber(CardUtils.getLastFourDigits(card.getNumber()));
        payment.setUserId(user.getId());
        payment.setInvoiceNumber(invoiceNumber);
        payment.setSubscription(subscription);

        CardDTO cardDTO = cardService.searchByCardNumber(card.getNumber());

        if(cardDTO.getCardType().equals(CardType.DEBIT)) {
            Long accountId = cardDTO.getAccountId();
            accountService.debit(accountId, amount);
        }else {
            cardService.cardDebit(cardDTO.getId(), amount);
        }

        accountService.addCredit(accountBeerClubNumber, amount);

        payment.setStatus(PaymentStatus.APROBADO);
        paymentRepository.save(payment);

        return payment;
    }

    @Override
    public void paymentValidation(Long subscriptionId, String cardHolder, String cardNumber,String cvv, String expDate ) throws EntityInactiveException, NotFoundException, BadRequestException, InsufficientBalanceException {
        String accountBeerClubNumber = myAppConfig.getAccountNumber();
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(() -> new NotFoundException("Subscription not found"));

        Double amount = subscription.getPrice();
        cardValidation(cardHolder, cardNumber, TransformationUtils.getNumber(cvv), expDate);

        CardDTO cardDTO = cardService.searchByCardNumber(cardNumber);
        CardType cardType = cardDTO.getCardType();

        if(cardType.equals(CardType.CREDIT)) {
            if(cardDTO.getCreditLimit() < amount ){
                throw new InsufficientBalanceException("La tarjeta no tiene crédito suficiente");
            }
        }else {
            Long accountId = cardDTO.getAccountId();
            accountValidation(accountId, amount);
        }

        try{
            accountService.searchByAccountNumber(accountBeerClubNumber);
        }catch (NotFoundException e) {
            throw new NotFoundException("La cuenta de destino no está disponible");
        }

    }

    // Método para crear una factura de pago para usuarios ya registrados.
    @Override
    public PaymentDTO createPaymentInvoice(Long userId) throws NotFoundException {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("No se encontró el usuario con ID: " + userId));

        if (!user.isActive()) {
            throw new NotFoundException("El usuario con ID " + userId + " no está activo");
        }

        Subscription subscription = subscriptionRepository.findById(user.getSubscription().getId()).orElseThrow(() -> new NotFoundException("No se encontró la suscripción"));

        Double amount = subscription.getPrice();
        String description = subscription.getName();
        String invoiceNumber = AccountUtils.getInvoiceNumber();

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setDescription(description);
        payment.setUserId(userId);
        payment.setInvoiceNumber(invoiceNumber);
        payment.setSubscription(subscription);

        paymentRepository.save(payment);
        return paymentMapper.converter(payment, PaymentDTO.class);
    }

    // Método para terminar de procesar el pago luego de que el usuario recibiera el link de la factura.
    @Override
    public PaymentDTO processPayment(PaymentApplicationDTO paymentDTO) throws NotFoundException, InsufficientBalanceException, EntityInactiveException, BadRequestException {
        String accountBeerClubNumber = myAppConfig.getAccountNumber();
        Payment payment = paymentRepository.findById(paymentDTO.getPaymentId()).orElseThrow(() -> new NotFoundException("No se encontró el pago"));

        try {
            this.paymentValidation(payment.getSubscription().getId(), paymentDTO.getCardHolder(), paymentDTO.getCardNumber(), paymentDTO.getCvv(), paymentDTO.getExpDate());

            payment.setDate(LocalDateTime.now());
            payment.setCardNumber(CardUtils.getLastFourDigits(paymentDTO.getCardNumber()));

            Double amount = payment.getSubscription().getPrice();
            CardDTO cardDTO = cardService.searchByCardNumber(paymentDTO.getCardNumber());

            if (cardDTO.getCardType().equals(CardType.DEBIT)) {
                Long accountId = cardDTO.getAccountId();
                accountService.debit(accountId, amount);
            } else {
                cardService.cardDebit(cardDTO.getId(), amount);
            }

            accountService.addCredit(accountBeerClubNumber, amount);

            payment.setStatus(PaymentStatus.APROBADO);
            paymentRepository.save(payment);

            return paymentMapper.converter(payment, PaymentDTO.class);
        } catch (NotFoundException | InsufficientBalanceException | EntityInactiveException | BadRequestException e) {
            payment.setStatus(PaymentStatus.RECHAZADO);
            paymentRepository.save(payment);
            throw e;
        }
    }

    // Método para que el administrador pueda cambiar el estado de un pago.
    @Override
    public PaymentDTO updatePaymentStatus(Long paymentId, PaymentStatus newStatus) throws NotFoundException {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new NotFoundException("Payment not found with ID: " + paymentId));
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        return paymentMapper.converter(payment, PaymentDTO.class);
    }

    private void cardValidation(String cardHolder, String number, int cvv, String expDate) throws NotFoundException, EntityInactiveException, BadRequestException {
        CardDTO cardDTO = cardService.searchByCardNumber(number);
        LocalDate expDateTime = CardUtils.parseStringToLocalDate(expDate);

        if(!cardDTO.getIsActive()) {
            throw new EntityInactiveException("La tarjeta no está activa");
        }
        if(!expDateTime.equals(cardDTO.getExpirationDate())) {
            throw new BadRequestException("La fecha de vencimiento de la tarjeta no es correcta");
        }
        if(!cardHolder.equals(cardDTO.getCardHolderName())) {
            throw new BadRequestException("El nombre del titular de la tarjeta no coincide");
        }
        if(cvv != cardDTO.getCvv()) {
            throw new BadRequestException("El cvv de la tarjeta no es correcto");
        }

    }

    private void accountValidation(Long accountId, Double amount) throws NotFoundException, EntityInactiveException, InsufficientBalanceException {
        AccountDTO accountDTO = accountService.searchById(accountId);
        if(!accountDTO.getIsActive()) {
            throw new EntityInactiveException("La cuenta no está activa");
        }
        if(accountDTO.getBalance() < amount) {
            throw new InsufficientBalanceException("La cuenta no tiene saldo suficiente");
        }

    }
}

