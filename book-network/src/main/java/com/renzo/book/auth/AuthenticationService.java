package com.renzo.book.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.renzo.book.email.EmailService;
import com.renzo.book.email.EmailTemplateName;
import com.renzo.book.role.RoleRepository;
import com.renzo.book.security.JwtService;
import com.renzo.book.user.Token;
import com.renzo.book.user.TokenRepository;
import com.renzo.book.user.User;
import com.renzo.book.user.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(RegistrationRequest registrationRequest) throws MessagingException {
        var role = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        var user = User.builder()
                .email(registrationRequest.getEmail())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .firstname(registrationRequest.getFirstname())
                .lastname(registrationRequest.getLastname())
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(role))
                .build();

        userRepository.save(user);

        sendValidationEmail(user);

    }

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);

        // send email
        emailService.sendEmail(user.getEmail(), user.getUsername(), EmailTemplateName.ACTIVATE_ACCOUNT, activationUrl,
                newToken, "Activate Account");

    }

    private String generateAndSaveActivationToken(User user) {
        // generate token
        String generatedToken = generateActivationCode(6);

        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();

        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder stringBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            stringBuilder.append(characters.charAt(randomIndex));
        }

        return stringBuilder.toString();
    }

    public AuthenticationResponse authenticate(@Valid AuthenticationRequest authenticateRequest) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticateRequest.getEmail(),
                        authenticateRequest.getPassword()));
        System.out.println("Auth: " + auth);

        var claims = new HashMap<String, Object>();
        var user = ((User) auth.getPrincipal());
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles());
        claims.put("fullname", user.fullName());

        var jwtToken = jwtService.generateToken(claims, user);

        System.out.println("Token: " + jwtToken);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public void activateAccount(String token) throws MessagingException {
        var tokenEntity = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Token not found"));

        if (tokenEntity.getValidatedAt() != null) {
            throw new IllegalStateException("Token already validated");
        }

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            sendValidationEmail(tokenEntity.getUser());
            throw new RuntimeException("Token expired");
        }

        var user = tokenEntity.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenEntity.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(tokenEntity);
    }

}
