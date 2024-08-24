package com.renzo.book.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.util.HashMap;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;
import java.util.Map;

@Service
public class JwtService {

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public String generateToken(UserDetails userDetails) {

        return generateToken(new HashMap<>(), userDetails);

    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails) {

        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        var authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        return Jwts.builder()
                .claims().add(extraClaims).add("authorities", authorities)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .and()
                .signWith(getSignInKey()).compact();

    }

    public String extractEmail(String jwt) {
        return extractClaim(jwt, Claims::getSubject);
    }

    private <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(jwt);
        return claimsResolver.apply(claims);

    }

    private Claims extractAllClaims(String jwt) {
        return Jwts.parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractEmail(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));

    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Key getSignInKey() {
        System.out.println("JwtService.generateToken");
        System.out.println(secretKey);
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);

    }

}
