package com.mag.service;

import com.mag.auth.AuthenticationRequest;
import com.mag.auth.AuthenticationResponse;
import com.mag.auth.RegisterRequest;
import com.mag.config.JwtService;
import com.mag.user.RoleEnum;
import com.mag.user.UserEntity;
import com.mag.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        final UserEntity user = UserEntity
                .builder()
                .firstName(request.getFirstname())
                .lastName(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RoleEnum.USER)
                .build();

        userRepository.save(user);

        final var token = jwtService.generateToken(user);

        return new AuthenticationResponse(token);
    }

    public AuthenticationResponse login(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        final var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        final var token = jwtService.generateToken(user);

        return new AuthenticationResponse(token);
    }

}
