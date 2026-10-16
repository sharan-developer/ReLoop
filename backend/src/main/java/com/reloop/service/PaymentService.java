package com.reloop.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.reloop.config.AppProperties;
import com.reloop.dto.Dtos.PaymentConfigDto;
import com.reloop.dto.Dtos.RazorpayVerifyRequest;
import com.reloop.entity.CustomerOrder;
import com.reloop.entity.Enums.*;
import com.reloop.entity.Payment;
import com.reloop.exception.ApiException;
import com.reloop.repository.CustomerOrderRepository;
import com.reloop.repository.PaymentRepository;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Razorpay integration. Orders are created server-side and payment signatures are verified server-side
 * (HMAC-SHA256 of "order_id|payment_id" with the key secret). A frontend success callback alone never marks anything paid.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final AppProperties props;
    private final PaymentRepository payments;
    private final CustomerOrderRepository orders;
    private final StorageService storage;

    public boolean enabled() { return props.getRazorpay().enabled(); }

    public PaymentConfigDto config() {
        return new PaymentConfigDto(enabled(), enabled() ? props.getRazorpay().getKeyId() : null, storage.cloudinaryEnabled());
    }

    /** Creates the gateway order and a PENDING payment row. Must run inside the caller's transaction. */
    public String createGatewayOrder(CustomerOrder order) {
        if (!enabled()) throw ApiException.badRequest("Online payments are not configured. Please choose Cash on Delivery.");
        try {
            RazorpayClient client = new RazorpayClient(props.getRazorpay().getKeyId(), props.getRazorpay().getKeySecret());
            JSONObject req = new JSONObject();
            req.put("amount", order.getTotal().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact());
            req.put("currency", "INR");
            req.put("receipt", order.getOrderNumber());
            com.razorpay.Order rzp = client.orders.create(req);
            String rid = rzp.get("id");
            Payment p = new Payment();
            p.setOrderRef(order);
            p.setProvider(PaymentMethod.RAZORPAY);
            p.setProviderOrderId(rid);
            p.setStatus(PaymentStatus.PENDING);
            p.setAmount(order.getTotal());
            payments.save(p);
            return rid;
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not start the online payment. Please try again or choose Cash on Delivery.");
        }
    }

    public String pendingGatewayOrderId(Long orderId) {
        return payments.findFirstByOrderRefIdOrderByIdDesc(orderId)
                .filter(p -> p.getProvider() == PaymentMethod.RAZORPAY && p.getStatus() == PaymentStatus.PENDING)
                .map(Payment::getProviderOrderId).orElse(null);
    }

    /** Verifies the signature and marks the payment PAID. Idempotent for already-paid orders. */
    @Transactional
    public CustomerOrder verify(Long userId, RazorpayVerifyRequest r) {
        if (!enabled()) throw ApiException.badRequest("Online payments are not configured");
        CustomerOrder order = orders.findByIdAndUserId(r.orderId(), userId).orElseThrow(() -> ApiException.notFound("Order"));
        Payment pay = payments.findFirstByOrderRefIdOrderByIdDesc(order.getId()).orElseThrow(() -> ApiException.notFound("Payment"));
        if (pay.getStatus() == PaymentStatus.PAID) return order;
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) throw ApiException.conflict("This order is not awaiting payment");
        if (!r.razorpayOrderId().equals(pay.getProviderOrderId())) throw ApiException.badRequest("Payment does not match this order");
        if (!signatureValid(r.razorpayOrderId(), r.razorpayPaymentId(), r.razorpaySignature())) {
            log.warn("Invalid Razorpay signature for order {}", order.getOrderNumber());
            throw ApiException.badRequest("Payment verification failed");
        }
        pay.setStatus(PaymentStatus.PAID);
        pay.setProviderPaymentId(r.razorpayPaymentId());
        payments.save(pay);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PLACED);
        return orders.save(order);
    }

    boolean signatureValid(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.getRazorpay().getKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : h) hex.append(String.format("%02x", b));
            return MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.UTF_8), signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
