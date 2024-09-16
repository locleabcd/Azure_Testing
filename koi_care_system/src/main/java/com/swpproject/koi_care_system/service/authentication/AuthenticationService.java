package com.swpproject.koi_care_system.service.authentication;


import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.swpproject.koi_care_system.dto.UserDTO;
import com.swpproject.koi_care_system.enums.ErrorCode;
import com.swpproject.koi_care_system.exceptions.AppException;
import com.swpproject.koi_care_system.mapper.UserMapper;
import com.swpproject.koi_care_system.models.User;
import com.swpproject.koi_care_system.payload.request.AuthenticationRequest;
import com.swpproject.koi_care_system.payload.request.IntrospectRequest;
import com.swpproject.koi_care_system.payload.response.AuthenticationResponse;
import com.swpproject.koi_care_system.payload.response.IntrospectResponse;
import com.swpproject.koi_care_system.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService implements IAuthenticationService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByUsername(request.getUsername()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var token = generateToken(user);
        UserDTO userDTO = userMapper.maptoUserDTO(user);
        return AuthenticationResponse.builder()
                .id(userDTO.getId())
                .username(userDTO.getUsername())
                .roles(userDTO.getRoles())
                .token(token)
                .isAuthenticated(true)
                .build();
    }

    public String generateToken(User user) {
        // Create HMAC signer
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // Create JWT claims set
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("phuoc.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(3, ChronoUnit.MINUTES).toEpochMilli()
                ))
                .claim("scope", buildScope(user))
                .build();

        // Create the payload
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        // Create the JWS object and sign it
        //JWT  three parts: header, payload, signature
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Error generate Token", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();

        // Create HMAC verifier with Signer Key
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);
        return IntrospectResponse.builder()
                .valid(verified && expirationTime.after(new Date()))//check if the token is valid and not expired
                .build();
    }

    private String buildScope(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new AppException(ErrorCode.NO_ROLES);
        }
        return user.getRoles().stream().findFirst().orElseThrow(() -> new AppException(ErrorCode.NO_ROLES));
    }
}


