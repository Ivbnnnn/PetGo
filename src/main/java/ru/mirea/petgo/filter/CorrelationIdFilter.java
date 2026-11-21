package ru.mirea.petgo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String MDC_KEY = "correlationId";
    private static final String HEADER = "X-Correlation-ID";
    private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        String id = received != null && VALID.matcher(received).matches() ? received : UUID.randomUUID().toString();
        response.setHeader(HEADER, id);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, id)) {
            chain.doFilter(request, response);
        }
    }
}
