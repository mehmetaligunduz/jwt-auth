package com.mag.resource;

import com.mag.common.request.AuthenticationRequest;
import com.mag.common.response.AuthenticationResponse;
import com.mag.common.request.RegisterRequest;
import com.mag.common.request.PasswordLessAuthenticationRequest;
import com.mag.common.request.PreAuthenticationRequest;
import com.mag.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/pre-login")
    public ResponseEntity<Boolean> preLogin(@RequestBody PreAuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.passwordLessLogin(request));
    }

    @PostMapping("/password-less-login")
    public ResponseEntity<AuthenticationResponse> passwordLessLogin(@RequestBody PasswordLessAuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.passwordLessLoginVerifying(request));
    }

}
