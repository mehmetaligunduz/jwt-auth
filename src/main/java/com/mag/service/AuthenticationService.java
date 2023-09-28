package com.mag.service;

import com.mag.common.request.PreAuthenticationRequest;
import com.mag.common.request.AuthenticationRequest;
import com.mag.common.request.MessageRequest;
import com.mag.common.request.PasswordLessAuthenticationRequest;
import com.mag.common.request.RegisterRequest;
import com.mag.common.response.AuthenticationResponse;
import com.mag.user.RoleEnum;
import com.mag.user.UserEntity;
import com.mag.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    private final WebClient.Builder webClientBuilder;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public static final String OTP_SERVICE_URL = "http://localhost:8080/otp-service/v1/";

    public static final String OTP_VERIFY_SERVICE_URL = "http://localhost:8080/otp-service/v1/verify";

    public static final String MQ_PUBLISH_URL = "http://localhost:8082/mq-service/publish";

    public AuthenticationResponse register(RegisterRequest request) {
        final UserEntity user = UserEntity
                .builder()
                .firstName(request.getFirstname())
                .lastName(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
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

    public Boolean passwordLessLogin(PreAuthenticationRequest request) {

        Optional<UserEntity> user = userRepository.findByEmail(request.getEmail());

        if (user.isEmpty()) {
            return Boolean.FALSE;
        }

        String code = webClientBuilder.build()
                .post()
                .uri(OTP_SERVICE_URL + user.get().getPhone())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        webClientBuilder.build()
                .post()
                .uri(MQ_PUBLISH_URL)
                .body(BodyInserters.fromValue(new MessageRequest(code, user.get().getPhone())))
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        logger.info(code);

        return true;
    }

    public AuthenticationResponse passwordLessLoginVerifying(PasswordLessAuthenticationRequest request) {

        Optional<UserEntity> user = userRepository.findByEmail(request.getOwner());

        if (Boolean.FALSE.equals(user.isPresent())) {
            return new AuthenticationResponse(null);
        }

        final Boolean isVerified = webClientBuilder.build()
                .post()
                .uri(OTP_VERIFY_SERVICE_URL)
                .body(BodyInserters.fromValue(new PasswordLessAuthenticationRequest(user.get().getPhone(), request.getCode())))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();

        if (Boolean.FALSE.equals(isVerified)) {
            return new AuthenticationResponse(null);
        }

        final var token = jwtService.generateToken(user.get());
        return new AuthenticationResponse(token);
    }
}
