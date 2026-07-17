package com.kushan.cource_svc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.kushan.cource_svc.model.Course;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.api-key}")
    private String apiKey;
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;
    @Value("${stripe.currency:usd}")
    private String currency;
    @Value("${app.frontend-base:http://localhost:5500}")
    private String frontendBase;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    public record CheckoutResult(String sessionId, String url) {}

    /** Creates a Stripe Checkout Session for a paid course enrollment. */
    public CheckoutResult createCheckout(Course course, String studentEmail, Long paymentId, Long courseId) {
        long cents = BigDecimal.valueOf(course.getPrice() == null ? 0 : course.getPrice())
                .multiply(BigDecimal.valueOf(100)).longValue();

        String success = frontendBase + "/course.html?id=" + courseId + "&enrolled=1&session_id={CHECKOUT_SESSION_ID}";
        String cancel = frontendBase + "/course.html?id=" + courseId + "&canceled=1";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(success)
                .setCancelUrl(cancel)
                .setCustomerEmail(studentEmail)
                .putMetadata("courseId", String.valueOf(courseId))
                .putMetadata("studentId", studentEmail)
                .putMetadata("paymentId", String.valueOf(paymentId))
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(cents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(course.getTitle())
                                        .setDescription(course.getSummary())
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            return new CheckoutResult(session.getId(), session.getUrl());
        } catch (Exception e) {
            throw new RuntimeException("Stripe checkout failed: " + e.getMessage(), e);
        }
    }

    /** True when the checkout session has been paid. Used by the dev confirm fallback. */
    public boolean isSessionPaid(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            String status = session.getPaymentStatus();
            return "paid".equalsIgnoreCase(status);
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve Stripe session: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies the Stripe webhook signature and extracts the fields we need.
     * Returns null when the signature is invalid. Using Jackson (not Stripe's
     * Event object graph) keeps this resilient across SDK versions.
     */
    public Map<String, String> parseWebhook(byte[] payload, String sigHeader) {
        try {
            String payloadStr = new String(payload, StandardCharsets.UTF_8);
            Event event = Webhook.constructEvent(payloadStr, sigHeader, webhookSecret);
            JsonNode root = mapper.readTree(payloadStr);
            String type = root.path("type").asText();
            JsonNode obj = root.path("data").path("object");
            String sessionId = obj.path("id").asText(null);
            String paymentStatus = obj.path("payment_status").asText(null);
            JsonNode meta = obj.path("metadata");
            String courseId = meta.path("courseId").asText(null);
            String studentId = meta.path("studentId").asText(null);
            return Map.of(
                    "type", type,
                    "sessionId", sessionId == null ? "" : sessionId,
                    "paymentStatus", paymentStatus == null ? "" : paymentStatus,
                    "courseId", courseId == null ? "" : courseId,
                    "studentId", studentId == null ? "" : studentId
            );
        } catch (SignatureVerificationException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Stripe webhook: " + e.getMessage(), e);
        }
    }
}
