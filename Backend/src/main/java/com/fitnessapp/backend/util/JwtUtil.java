package com.fitnessapp.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            logger.error("Fehler beim Parsen des Tokens: {}", e.getMessage());
            throw new RuntimeException("Token konnte nicht validiert werden.");
        }
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (username == null || userDetails == null || userDetails.getUsername() == null) {
            logger.error("Token-Validierung fehlgeschlagen: Benutzername oder UserDetails sind null");
            return false;
        }
        boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        if (!isValid) {
            logger.error("Token-Validierung fehlgeschlagen: Benutzername stimmt nicht oder Token ist abgelaufen");
        }
        return isValid;
    }

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role == null || role.isEmpty() ? "USER" : role);
        return createToken(claims, username, accessTokenValidity);
    }

    public String generateRefreshToken(String username) {
        return createToken(new HashMap<>(), username, refreshTokenValidity);
    }

    private String createToken(Map<String, Object> claims, String subject, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        try {
            byte[] keyBytes = Hex.decodeHex(secret.toCharArray());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (DecoderException e) {
            logger.error("Fehler bei der Dekodierung des Secret-Schlüssels: {}", e.getMessage());
            throw new RuntimeException("Ungültiger Secret-Key");
        }
    }
}
