package com.goaltracker.security;
import java.nio.charset.StandardCharsets;
 import java.util.Date;

 import javax.crypto.SecretKey;

 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Service;

 import com.goaltracker.model.Role;

 import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
@Service public class JwtService { private final SecretKey key; private final long expiration; public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-ms}") long expiration){if(secret.length()<32)throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters");key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.expiration=expiration;} public String create(Long id,String email,Role role){return Jwts.builder().subject(email).claim("uid",id).claim("role",role.name()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expiration)).signWith(key).compact();} public String email(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();} public Role role(String token){return Role.valueOf((String)Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("role"));} public boolean valid(String token){try{email(token);role(token);return true;}catch(JwtException|IllegalArgumentException e){return false;}} }
