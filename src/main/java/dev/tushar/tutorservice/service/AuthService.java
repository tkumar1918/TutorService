package dev.tushar.tutorservice.service;

import dev.tushar.tutorservice.dto.request.AuthRequestDTO;
import dev.tushar.tutorservice.dto.request.RefreshTokenRequestDTO;
import dev.tushar.tutorservice.dto.request.RegisterRequestDTO;
import dev.tushar.tutorservice.dto.response.LoginResponseDTO;
import dev.tushar.tutorservice.dto.response.RefreshTokenResponseDTO;
import dev.tushar.tutorservice.dto.response.RegisterResponseDTO;
import dev.tushar.tutorservice.entity.Permission;
import dev.tushar.tutorservice.entity.RefreshToken;
import dev.tushar.tutorservice.entity.Role;
import dev.tushar.tutorservice.entity.User;
import dev.tushar.tutorservice.exception.ApiException;
import dev.tushar.tutorservice.repository.PermissionRepository;
import dev.tushar.tutorservice.repository.RefreshTokenRepository;
import dev.tushar.tutorservice.repository.RoleRepository;
import dev.tushar.tutorservice.repository.UserRepository;
import dev.tushar.tutorservice.security.CustomUserDetails;
import dev.tushar.tutorservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

        private final JwtUtil jwtUtil;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PermissionRepository permissionRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final RefreshTokenRepository refreshTokenRepository;
        private final StringRedisTemplate redisTemplate;

        @Value("${max-sessions:3}")
        private int maxSessions;

        @Value("${refresh-token-expiration-time:2592000000}") // 30 days
        private long refreshTokenExpirationTime;

        private static final String SESSION_PREFIX = "session:";

        public RegisterResponseDTO register(RegisterRequestDTO request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "An account with the provided email already exists.");
                }

                Role userRole = roleRepository.findByName("USER")
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Server configuration error: The 'USER' role is missing."));

                // Simplified registration without specific permission checks for now
                // Assuming every user gets the default 'USER' role

                User user = User.builder()
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .roles(Set.of(userRole))
                                // .permissions(Set.of(createBusinessPermission)) // Removed
                                .build();

                userRepository.save(user);
                return new RegisterResponseDTO(
                                user.getId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail());
        }

        public LoginResponseDTO authenticate(AuthRequestDTO request, String ipAddress, String deviceInfo) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

                // When a user exceeds the max session limit, they can choose an old session to
                // invalidate.
                if (request.getSessionIdToInvalidate() != null) {
                        RefreshToken tokenToInvalidate = refreshTokenRepository
                                        .findById(request.getSessionIdToInvalidate())
                                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                        "Session to invalidate not found."));

                        if (!tokenToInvalidate.getUser().getId().equals(user.getId())) {
                                throw new AccessDeniedException(
                                                "You do not have permission to invalidate this session.");
                        }

                        // Then, we will invalidate the session in both Redis and the database
                        // Invalidate the redis one will restrict any future requests from the
                        // previously generated auth token
                        redisTemplate.delete(SESSION_PREFIX + tokenToInvalidate.getId().toString());
                        refreshTokenRepository.delete(tokenToInvalidate);

                        return performLogin(user, ipAddress, deviceInfo);
                }

                List<RefreshToken> activeSessions = refreshTokenRepository.findByUser(user);
                if (activeSessions.size() >= maxSessions) {
                        List<Map<String, Object>> sessionDetails = activeSessions.stream()
                                        .map(rt -> {
                                                Map<String, Object> map = new HashMap<>();
                                                map.put("id", rt.getId());
                                                map.put("deviceInfo", rt.getDeviceInfo());
                                                map.put("ipAddress", rt.getIpAddress());
                                                map.put("createdAt", rt.getCreatedAt());
                                                return map;
                                        })
                                        .collect(Collectors.toList());
                        return new LoginResponseDTO(sessionDetails);
                }

                return performLogin(user, ipAddress, deviceInfo);
        }

        public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
                RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token."));

                if (oldRefreshToken.getExpiryDate().isBefore(Instant.now())) {
                        redisTemplate.delete(SESSION_PREFIX + oldRefreshToken.getId().toString());
                        refreshTokenRepository.delete(oldRefreshToken);
                        throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has expired.");
                }

                User user = oldRefreshToken.getUser();

                // Alright, time to kill the session for good.
                // First, we yank the session from Redis. This makes the current access token
                // instantly useless.
                redisTemplate.delete(SESSION_PREFIX + oldRefreshToken.getId().toString());

                // Then, we delete the actual refresh token from the database
                // so they can't use it to get a new token.
                refreshTokenRepository.delete(oldRefreshToken);

                LoginResponseDTO newSession = performLogin(user, oldRefreshToken.getIpAddress(),
                                oldRefreshToken.getDeviceInfo());

                return new RefreshTokenResponseDTO(
                                newSession.jwt(),
                                newSession.refreshToken(),
                                newSession.refreshTokenExpiresIn());
        }

        public void logout(String accessToken) {
                String rti = jwtUtil.extractRti(accessToken);
                if (rti != null) {
                        // Invalidate the RefreshToken's ID in Redis (this will prevent any further
                        // requests from already generated auth token)
                        // It acts as a flag to indicate that the session is no longer active
                        redisTemplate.delete(SESSION_PREFIX + rti);

                        // Invalidate the refresh token in the database
                        // Prevent any further requests for getting the new auth token
                        refreshTokenRepository.findById(UUID.fromString(rti))
                                        .ifPresent(refreshTokenRepository::delete);
                }
        }

        public boolean isSessionActive(String rti) {
                if (rti == null) {
                        return false;
                }
                return Boolean.TRUE.equals(redisTemplate.hasKey(SESSION_PREFIX + rti));
        }

        // --- Private Helper Methods ---

        private LoginResponseDTO performLogin(User user, String ipAddress, String deviceInfo) {
                // First, create and save the RefreshToken to the database to get its unique ID
                RefreshToken refreshToken = RefreshToken.builder()
                                .user(user)
                                .token(UUID.randomUUID().toString()) // The secure random string for refreshing
                                .expiryDate(Instant.now().plus(refreshTokenExpirationTime, ChronoUnit.MILLIS))
                                .ipAddress(ipAddress)
                                .deviceInfo(deviceInfo)
                                .build();
                refreshToken = refreshTokenRepository.save(refreshToken);
                UUID newRefreshTokenId = refreshToken.getId(); // This UUID is our session identifier

                // Store the refresh token's UUID (which acts as our session ID) in Redis.
                // This creates a fast, centralized record of all active user sessions.
                // The JwtAuthenticationFilter will check for the existence of this key on every
                // incoming request.
                // If this key does not exist in Redis, it means the session has been terminated
                // (e.g., via logout or session termination), and the access token must be
                // rejected,
                // even if it hasn't technically expired yet.
                redisTemplate.opsForValue().set(
                                SESSION_PREFIX + newRefreshTokenId.toString(),
                                "active",
                                Duration.ofMillis(refreshTokenExpirationTime));

                // Generate the JWT, embedding the session ID (rti) so it's linked to the Redis
                // entry
                String jwtToken = jwtUtil.generateToken(
                                new CustomUserDetails(user),
                                newRefreshTokenId.toString());

                return new LoginResponseDTO(
                                user.getId(),
                                user.getFirstName(),
                                user.getEmail(),
                                new HashMap<>(Map.of("token", jwtToken, "expiresIn",
                                                jwtUtil.getAccessTokenExpirationTime())),
                                refreshToken.getToken(),
                                refreshTokenExpirationTime);
        }
}
