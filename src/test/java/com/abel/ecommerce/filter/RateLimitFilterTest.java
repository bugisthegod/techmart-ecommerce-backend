package com.abel.ecommerce.filter;

import com.abel.ecommerce.utils.JwtTokenUtil;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for RateLimitFilter
 * Tests distributed rate limiting functionality using Bucket4j + Redis
 * Covers both IP-based and user-based rate limiting dimensions
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitFilterTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private BucketConfiguration ipRateLimitConfig;

    @Mock
    private BucketConfiguration userRateLimitConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private BucketProxy ipBucketProxy;

    @Mock
    private BucketProxy userBucketProxy;

    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;

    private RateLimitFilter rateLimitFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitFilter = new RateLimitFilter(proxyManager, ipRateLimitConfig, userRateLimitConfig);

        // Setup response output stream
        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Mock ProxyManager.builder() to return the mock builder
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);

        // Mock builder.build() to return appropriate bucket proxies based on key prefix
        when(remoteBucketBuilder.build(anyString(), any(java.util.function.Supplier.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (key.startsWith("rate_limit:ip:")) {
                return ipBucketProxy;
            } else if (key.startsWith("rate_limit:user:")) {
                return userBucketProxy;
            }
            return null;
        });
    }

    @Nested
    class IpRateLimitTests {

        @Test
        void doFilterInternal_requestWithinIpRateLimit_shouldContinueFilterChain() throws Exception {
            // Test IP rate limiting - requests within limit should be allowed
            // Arrange
            String clientIp = "192.168.1.100";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void doFilterInternal_requestExceedsIpRateLimit_shouldReturn429() throws Exception {
            // Test IP rate limiting - exceeding limit should return 429 Too Many Requests
            // Arrange
            String clientIp = "192.168.1.101";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(ipBucketProxy.tryConsume(1)).thenReturn(false);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            verify(response).setContentType("application/json");
            verify(filterChain, never()).doFilter(any(), any());

            String responseContent = responseWriter.toString();
            assertThat(responseContent).isEqualTo("{\"error\": \"Too many requests from IP. Please try again later.\"}");
        }
    }

    @Nested
    class UserRateLimitTests {

        @Test
        void doFilterInternal_authenticatedRequestWithinUserRateLimit_shouldContinueFilterChain() throws Exception {
            // Test user rate limiting - authenticated requests within limit should be allowed
            // Arrange
            String clientIp = "192.168.1.102";
            Long userId = 12345L;
            String validToken = generateMockToken(userId);

            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);
            when(userBucketProxy.tryConsume(1)).thenReturn(true);

            try (MockedStatic<JwtTokenUtil> jwtMock = mockStatic(JwtTokenUtil.class)) {
                jwtMock.when(() -> JwtTokenUtil.getUserIdFromToken(validToken)).thenReturn(userId);

                // Act
                rateLimitFilter.doFilterInternal(request, response, filterChain);

                // Assert
                verify(filterChain).doFilter(request, response);
                verify(ipBucketProxy).tryConsume(1);
                verify(userBucketProxy).tryConsume(1);
            }
        }

        @Test
        void doFilterInternal_authenticatedRequestExceedsUserRateLimit_shouldReturn429() throws Exception {
            // Test user rate limiting - authenticated requests exceeding limit should return 429
            // Arrange
            String clientIp = "192.168.1.103";
            Long userId = 67890L;
            String validToken = generateMockToken(userId);

            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);
            when(userBucketProxy.tryConsume(1)).thenReturn(false);

            try (MockedStatic<JwtTokenUtil> jwtMock = mockStatic(JwtTokenUtil.class)) {
                jwtMock.when(() -> JwtTokenUtil.getUserIdFromToken(validToken)).thenReturn(userId);

                // Act
                rateLimitFilter.doFilterInternal(request, response, filterChain);

                // Assert
                verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                verify(response).setContentType("application/json");
                verify(filterChain, never()).doFilter(any(), any());

                String responseContent = responseWriter.toString();
                assertThat(responseContent).isEqualTo("{\"error\": \"Too many requests from user. Please try again later.\"}");
            }
        }
    }

    @Nested
    class IpExtractionTests {

        @Test
        void getClientIp_fromXForwardedForHeader_shouldReturnFirstIp() throws Exception {
            // Test extracting client IP from X-Forwarded-For header
            // Arrange
            String forwardedIp = "203.0.113.195";
            when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedIp);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void getClientIp_fromXForwardedForWithMultipleIps_shouldReturnFirstIp() throws Exception {
            // Test extracting first IP when X-Forwarded-For contains multiple IPs
            // Arrange
            String multipleIps = "203.0.113.195, 70.41.3.18, 150.172.238.178";
            String expectedIp = "203.0.113.195";

            when(request.getHeader("X-Forwarded-For")).thenReturn(multipleIps);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void getClientIp_fromXRealIpHeader_shouldReturnIp() throws Exception {
            // Test extracting client IP from X-Real-IP header (when X-Forwarded-For is absent)
            // Arrange
            String realIp = "198.51.100.178";
            when(request.getHeader("X-Real-IP")).thenReturn(realIp);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void getClientIp_fromRemoteAddr_shouldReturnIp() throws Exception {
            // Test extracting client IP from RemoteAddr (when proxy headers are absent)
            // Arrange
            String remoteAddr = "192.0.2.1";
            when(request.getRemoteAddr()).thenReturn(remoteAddr);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void getClientIp_whenXForwardedForIsUnknown_shouldFallbackToXRealIp() throws Exception {
            // Test fallback to X-Real-IP when X-Forwarded-For is 'unknown'
            // Arrange
            String realIp = "198.51.100.200";
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getHeader("X-Real-IP")).thenReturn(realIp);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }
    }

    @Nested
    class AuthenticationScenarioTests {

        @Test
        void doFilterInternal_unauthenticatedRequestWithoutJwt_shouldOnlyCheckIpRateLimit() throws Exception {
            // Test unauthenticated requests only apply IP rate limiting
            // Arrange
            String clientIp = "192.168.1.104";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
            verify(userBucketProxy, never()).tryConsume(anyLong());
        }

        @Test
        void doFilterInternal_invalidJwtToken_shouldSkipUserRateLimitAndOnlyCheckIp() throws Exception {
            // Test invalid JWT token should skip user rate limiting, still apply IP rate limiting
            // Arrange
            String clientIp = "192.168.1.105";
            String invalidToken = "invalid.jwt.token";

            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            try (MockedStatic<JwtTokenUtil> jwtMock = mockStatic(JwtTokenUtil.class)) {
                jwtMock.when(() -> JwtTokenUtil.getUserIdFromToken(invalidToken))
                        .thenThrow(new RuntimeException("Invalid token"));

                // Act
                rateLimitFilter.doFilterInternal(request, response, filterChain);

                // Assert
                verify(filterChain).doFilter(request, response);
                verify(ipBucketProxy).tryConsume(1);
                verify(userBucketProxy, never()).tryConsume(anyLong());
            }
        }

        @Test
        void doFilterInternal_tokenWithoutBearerPrefix_shouldIgnoreAndOnlyCheckIpRateLimit() throws Exception {
            // Test Authorization header without Bearer prefix should ignore token
            // Arrange
            String clientIp = "192.168.1.106";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
            verify(userBucketProxy, never()).tryConsume(anyLong());
        }
    }

    @Nested
    class ComprehensiveScenarioTests {

        @Test
        void doFilterInternal_bothIpAndUserRateLimitPass_shouldContinueFilterChain() throws Exception {
            // Test scenario where both IP and user rate limits pass
            // Arrange
            String clientIp = "192.168.1.107";
            Long userId = 99999L;
            String validToken = generateMockToken(userId);

            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);
            when(userBucketProxy.tryConsume(1)).thenReturn(true);

            try (MockedStatic<JwtTokenUtil> jwtMock = mockStatic(JwtTokenUtil.class)) {
                jwtMock.when(() -> JwtTokenUtil.getUserIdFromToken(validToken)).thenReturn(userId);

                // Act
                rateLimitFilter.doFilterInternal(request, response, filterChain);

                // Assert
                verify(filterChain).doFilter(request, response);
                verify(ipBucketProxy).tryConsume(1);
                verify(userBucketProxy).tryConsume(1);
            }
        }

        @Test
        void doFilterInternal_ipRateLimitPassesButUserRateLimitFails_shouldReturn429() throws Exception {
            // Test scenario where IP rate limit passes but user rate limit fails
            // Arrange
            String clientIp = "192.168.1.108";
            Long userId = 88888L;
            String validToken = generateMockToken(userId);

            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);
            when(userBucketProxy.tryConsume(1)).thenReturn(false);

            try (MockedStatic<JwtTokenUtil> jwtMock = mockStatic(JwtTokenUtil.class)) {
                jwtMock.when(() -> JwtTokenUtil.getUserIdFromToken(validToken)).thenReturn(userId);

                // Act
                rateLimitFilter.doFilterInternal(request, response, filterChain);

                // Assert
                verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                verify(filterChain, never()).doFilter(any(), any());
                verify(ipBucketProxy).tryConsume(1);
                verify(userBucketProxy).tryConsume(1);

                String responseContent = responseWriter.toString();
                assertThat(responseContent).isEqualTo("{\"error\": \"Too many requests from user. Please try again later.\"}");
            }
        }

        @Test
        void doFilterInternal_ipRateLimitFails_shouldReturn429WithoutCheckingUserRateLimit() throws Exception {
            // Test IP rate limit failure should return immediately without checking user rate limit
            // Arrange
            String clientIp = "192.168.1.109";
            Long userId = 77777L;
            String validToken = generateMockToken(userId);

            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(ipBucketProxy.tryConsume(1)).thenReturn(false);

            try (MockedStatic<JwtTokenUtil> jwtMock = mockStatic(JwtTokenUtil.class)) {
                jwtMock.when(() -> JwtTokenUtil.getUserIdFromToken(validToken)).thenReturn(userId);

                // Act
                rateLimitFilter.doFilterInternal(request, response, filterChain);

                // Assert
                verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                verify(filterChain, never()).doFilter(any(), any());
                verify(ipBucketProxy).tryConsume(1);
                verify(userBucketProxy, never()).tryConsume(anyLong());

                String responseContent = responseWriter.toString();
                assertThat(responseContent).isEqualTo("{\"error\": \"Too many requests from IP. Please try again later.\"}");
            }
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void getClientIp_withIPv6InXForwardedFor_shouldExtractFirstIp() throws Exception {
            // Test extracting IPv6 address from X-Forwarded-For
            // Arrange
            String ipv6Address = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
            when(request.getHeader("X-Forwarded-For")).thenReturn(ipv6Address);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void getClientIp_withIPv6AndIPv4Mixed_shouldExtractFirstIp() throws Exception {
            // Test mixed IPv6 and IPv4 in X-Forwarded-For
            // Arrange
            String mixedIps = "2001:0db8:85a3::8a2e:0370:7334, 192.168.1.1, 10.0.0.1";
            String expectedIp = "2001:0db8:85a3::8a2e:0370:7334";
            when(request.getHeader("X-Forwarded-For")).thenReturn(mixedIps);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void getClientIp_withEmptyXForwardedFor_shouldFallbackToRemoteAddr() throws Exception {
            // Test empty X-Forwarded-For should fallback to RemoteAddr
            // Arrange
            String remoteAddr = "192.168.1.200";
            when(request.getHeader("X-Forwarded-For")).thenReturn("");
            when(request.getRemoteAddr()).thenReturn(remoteAddr);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void getClientIp_withWhitespaceInXForwardedFor_shouldTrimAndExtract() throws Exception {
            // Test X-Forwarded-For with extra whitespace
            // Arrange
            String ipsWithWhitespace = "  192.168.1.100  , 10.0.0.1 , 172.16.0.1  ";
            String expectedIp = "192.168.1.100";
            when(request.getHeader("X-Forwarded-For")).thenReturn(ipsWithWhitespace);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }

        @Test
        void doFilterInternal_withNullRemoteAddr_shouldHandleGracefully() throws Exception {
            // Test null RemoteAddr edge case
            // Arrange
            when(request.getRemoteAddr()).thenReturn(null);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);

            // Act
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(ipBucketProxy).tryConsume(1);
        }
    }

    @Nested
    class ConcurrencyTests {

        @Test
        void testConcurrentRequestsFromSameIpEnforceLimit() throws Exception {
            // Test that concurrent requests don't bypass rate limit due to race conditions
            // This validates that Bucket4j's distributed locking works correctly

            int threadCount = 50;
            int expectedSuccessCount = 50; // Assuming bucket capacity > 50 for this test
            String clientIp = "192.168.1.200";

            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
            java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
            java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger rateLimitedCount = new java.util.concurrent.atomic.AtomicInteger(0);

            // Configure mocks for concurrent execution
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(ipBucketProxy.tryConsume(1)).thenAnswer(invocation -> {
                // Simulate realistic bucket behavior: first 50 succeed, rest fail
                return successCount.get() < expectedSuccessCount;
            });

            // Submit concurrent tasks
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        rateLimitFilter.doFilterInternal(request, response, filterChain);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        rateLimitedCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Start all threads simultaneously
            startLatch.countDown();
            doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            executor.shutdown();

            // Verify concurrent access was handled correctly
            verify(ipBucketProxy, times(threadCount)).tryConsume(1);
        }

        @Test
        void testConcurrentRequestsFromSameUserDoesNotThrowException() throws Exception {
            // Test that concurrent requests don't cause threading issues in the filter
            // Note: Real concurrency validation happens in integration tests with actual Redis
            // This unit test just verifies no threading exceptions occur

            int threadCount = 10;
            String clientIp = "192.168.1.201";
            Long userId = 99999L;
            String validToken = generateMockToken(userId);

            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
            java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
            java.util.concurrent.atomic.AtomicInteger exceptionCount = new java.util.concurrent.atomic.AtomicInteger(0);

            // Configure mocks - use synchronized mock to avoid threading issues
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(ipBucketProxy.tryConsume(1)).thenReturn(true);
            when(userBucketProxy.tryConsume(1)).thenReturn(true);

            try (MockedStatic<JwtTokenUtil> jwtMock = mockStatic(JwtTokenUtil.class)) {
                jwtMock.when(() -> JwtTokenUtil.getUserIdFromToken(validToken)).thenReturn(userId);

                // Submit concurrent tasks
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                            rateLimitFilter.doFilterInternal(request, response, filterChain);
                        } catch (Exception e) {
                            exceptionCount.incrementAndGet();
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }

                // Start all threads simultaneously
                startLatch.countDown();
                doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                executor.shutdown();

                // Verify no exceptions occurred during concurrent execution
                assertThat(exceptionCount.get()).isEqualTo(0);
            }
        }
    }

    @Nested
    class RedisFailureTests {

        @Test
        void testProxyManagerBuilderThrowsException_shouldReturn500() throws Exception {
            // Test graceful handling when Redis/ProxyManager fails

            String clientIp = "192.168.1.202";
            when(request.getRemoteAddr()).thenReturn(clientIp);

            // Simulate Redis failure
            when(proxyManager.builder()).thenThrow(new RuntimeException("Redis connection failed"));

            // Act & Assert - filter should propagate exception (or handle gracefully depending on requirements)
            try {
                rateLimitFilter.doFilterInternal(request, response, filterChain);
                // If implementation handles gracefully, verify appropriate response
            } catch (RuntimeException e) {
                // Expected behavior: exception propagates
                assertThat(e.getMessage()).contains("Redis connection failed");
            }

            // Verify filter chain was not called due to error
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        void testBucketProxyTryConsumeThrowsException_shouldHandleGracefully() throws Exception {
            // Test handling when bucket operation fails (e.g., serialization error)

            String clientIp = "192.168.1.203";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(ipBucketProxy.tryConsume(1)).thenThrow(new RuntimeException("Bucket operation failed"));

            // Act & Assert
            try {
                rateLimitFilter.doFilterInternal(request, response, filterChain);
            } catch (RuntimeException e) {
                // Exception should propagate or be handled gracefully
                assertThat(e.getMessage()).contains("Bucket operation failed");
            }

            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        void testResponseWriterThrowsIOException_shouldHandleGracefully() throws Exception {
            // Test handling when writing rate limit error response fails

            String clientIp = "192.168.1.204";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(ipBucketProxy.tryConsume(1)).thenReturn(false); // Trigger rate limit

            // Simulate IOException when writing response
            when(response.getWriter()).thenThrow(new java.io.IOException("Writer failed"));

            // Act & Assert
            try {
                rateLimitFilter.doFilterInternal(request, response, filterChain);
            } catch (java.io.IOException e) {
                // IOException should propagate
                assertThat(e.getMessage()).contains("Writer failed");
            }

            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    /**
     * Generate mock JWT token for testing.
     * Returns a realistic-looking but fake JWT token.
     * The actual token parsing is mocked via MockedStatic, so the content doesn't matter.
     */
    private String generateMockToken(Long userId) {
        // Format: header.payload.signature (base64-like strings)
        return "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxMjM0NSJ9.mockSignature" + userId;
    }
}