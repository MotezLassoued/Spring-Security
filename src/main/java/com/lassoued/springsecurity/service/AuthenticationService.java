package com.lassoued.springsecurity.service;

import com.lassoued.springsecurity.config.JwtService;
import com.lassoued.springsecurity.domain.*;
import com.lassoued.springsecurity.exception.UserAlreadyExistException;
import com.lassoued.springsecurity.repository.RefreshTokenRepository;
import com.lassoued.springsecurity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
            var user = User.builder()
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole() != null ? request.getRole() : Role.USER)
                    .enabled(true)
                    .accountLocked(false)
                    .build();

        try{
            var savedUser = userRepository.save(user);
            var accessToken = jwtService.generateToken(savedUser);
            var refreshToken = jwtService.generateRefreshToken(savedUser);

            saveRefreshToken(savedUser, refreshToken);
            return buildAuthResponse(accessToken, refreshToken, savedUser);

        }
        catch (Exception e){
            throw new UserAlreadyExistException("Error while saving user", e);
        }}

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveRefreshToken(user, refreshToken);

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    public AuthenticationResponse refreshToken(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String refreshToken = authHeader.substring(7);

        try {
            String userEmail = jwtService.extractUsernameFromRefreshToken(refreshToken);

            if (userEmail != null) {
                var user = userRepository.findByEmail(userEmail).orElseThrow();
                var refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken).orElse(null);

                if (refreshTokenEntity != null &&
                        !refreshTokenEntity.isRevoked() &&
                        !refreshTokenEntity.isExpired() &&
                        jwtService.isRefreshTokenValid(refreshToken, user)) {

                    var accessToken = jwtService.generateToken(user);

                    return buildAuthResponse(accessToken, refreshToken, user);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public void logout(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null) {
                var user = userRepository.findByEmail(userEmail).orElse(null);
                if (user != null) {
                    revokeAllUserTokens(user);
                }
            }
        } catch (Exception e) {
            // Token might be expired, just ignore
        }
    }

    private AuthenticationResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .permissions(user.getRole().getPermissions().stream()
                        .map(Permission::getPermission)
                        .collect(Collectors.toList()))
                .build();
    }

    private void saveRefreshToken(User user, String refreshToken) {
        var token = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(Instant.now().plusSeconds(604800))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user.getId());
    }

    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }
}
