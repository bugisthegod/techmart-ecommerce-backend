package com.abel.ecommerce.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.List;

public class JwtTokenUtil {
    /**
     * Token expiration time: 50 minutes
     */
    private static final long EXPIRE_TIME = 50 * 60 * 1000; // Fixed: was 5*60*10000

    /**
     * JWT secret key
     */
    private static final String SECRET = "ecommerce_secret_key"; // Changed to project-specific

    /**
     * Generate JWT token with 50 minutes expiration
     * @param username Username to be encoded in token
     * @param userId User ID to be encoded in token
     * @return JWT token string
     * @throws Exception If token generation fails
     */
    public static String generateToken(String username, Long userId) throws Exception {
        try {
            Date expirationDate = new Date(System.currentTimeMillis() + EXPIRE_TIME);
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            return JWT.create()
                    // Save username to token
                    .withAudience(username)
                    .withClaim("userId", userId)
                    // Token expires in 50 minutes
                    .withExpiresAt(expirationDate)
                    // Sign with secret key
                    .sign(algorithm);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Exception("Token generation failed");
        }
    }


    /**
     * Extract username from token
     * @param token JWT token
     * @return Username stored in token
     * @throws JWTDecodeException If token decoding fails
     */
    public static String getUsernameFromToken(String token) {
            DecodedJWT decode = JWT.decode(token);
            List<String> audience = decode.getAudience();
            if (audience == null || audience.isEmpty()){
                throw new JWTDecodeException("Invalid token format");
            }
            return audience.get(0);
    }

    /**
     * Extract userId from token
     * @param token JWT token
     * @return User ID stored in token, or null if not present
     */
    public static Long getUserIdFromToken(String token) {
        try {
            DecodedJWT decode = JWT.decode(token);
            return decode.getClaim("userId").asLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate JWT token
     * @param token JWT token to validate
     * @return true if token is valid, throws exception if invalid
     * @throws RuntimeException If token is invalid
     */
    public static boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Invalid token, please login again");
        }
    }

    /**
     * Get token expiration time in seconds
     * @return Expiration time in seconds
     */
    public static long getExpirationTimeInSeconds() {
        return EXPIRE_TIME / 1000;
    }
}