package com.ecommerce.backend.modules.payment.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.payment.config.VnPayConfig;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.payment.entity.TransactionLog;
import com.ecommerce.backend.modules.payment.repository.TransactionLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayServiceImpl implements VnPayService{

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final TransactionLogRepository transactionLogRepository;

    @Override
    public String createPaymentUrl(Long orderId, HttpServletRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid.");
        }

        // VNPAY quy định vnp_amount = số tiền thanh toán * 100
        long amount = order.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();

        String vnpTxnRef = String.valueOf(orderId);
        String vnpIpAddr = getIpAddress(request);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String vnpCreateDate = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String vnpExpireDate = now.plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang #" + orderId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", vnpIpAddr);
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        Iterator<Map.Entry<String, String>> iterator = vnpParams.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();

            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (iterator.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String vnpSecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return vnPayConfig.getPayUrl() + "?" + query;
    }

    @Transactional
    @Override
    public Map<String, Object> handleReturnUrl(HttpServletRequest request) {

        Map<String, String> vnpParams = extractVnpParams(request);

        String vnpSecureHash = request.getParameter("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String calculatedHash = hmacSHA512(
                vnPayConfig.getHashSecret(), buildHashData(vnpParams));

        Map<String, Object> result = new LinkedHashMap<>();

        if (!calculatedHash.equals(vnpSecureHash)) {
            log.warn("VNPAY Return - Invalid signature for TxnRef: {}",
                    vnpParams.get("vnp_TxnRef"));
            result.put("success", false);
            result.put("message",
                    "An error occurred while verifying your payment.");
            return result;
        }

        String responseCode = vnpParams.get("vnp_ResponseCode");
        String txnRef = vnpParams.get("vnp_TxnRef");
        Long orderId = Long.parseLong(txnRef);

        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            result.put("success", false);
            result.put("message", "Order not found.");
            return result;
        }

        saveTransactionLog(order, vnpParams, "00".equals(responseCode));

        if ("00".equals(responseCode)) {
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);
            }
            result.put("success", true);
            result.put("message", "Payment successful!");
            result.put("orderId", String.valueOf(orderId));
        } else if ("24".equals(responseCode)) {
            // Mã 24, khách hàng hủy thanh toán
            result.put("success", false);
            result.put("message",
                    "Payment cancelled. You can retry payment later.");
            result.put("orderId", String.valueOf(orderId));
        } else {
            // Giao dịch thất bại
            result.put("success", false);
            result.put("message",
                    "Payment failed. Please try another test card.");
            result.put("orderId", String.valueOf(orderId));
        }

        return result;
    }

    @Transactional
    @Override
    public Map<String, String> handleIpn(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();

        try {
            Map<String, String> vnpParams = extractVnpParams(request);
            // Kiểm tra checksum
            String vnpSecureHash = request.getParameter("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");

            String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), buildHashData(vnpParams));

            if (!calculatedHash.equals(vnpSecureHash)) {
                log.error("VNPAY IPN - Invalid checksum");
                result.put("RspCode", "97");
                result.put("Message", "Invalid Checksum");
                return result;
            }

            String vnpTmnCode = vnpParams.get("vnp_TmnCode");
            if (!vnPayConfig.getTmnCode().equals(vnpTmnCode)) {
                log.error("VNPAY IPN - TmnCode mismatch: {}", vnpTmnCode);
                result.put("RspCode", "99");
                result.put("Message", "Unknown error");
                return result;
            }

            String txnRef = vnpParams.get("vnp_TxnRef");
            Long orderId = Long.parseLong(txnRef);
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                result.put("RspCode", "01");
                result.put("Message", "Order Not Found");
                return result;
            }

            long vnpAmount = Long.parseLong(vnpParams.get("vnp_Amount")) / 100;
            if (order.getTotalPrice().compareTo(BigDecimal.valueOf(vnpAmount)) != 0) {
                // VNPAY retry
                result.put("RspCode", "04");
                result.put("Message", "Invalid Amount");
                return result;
            }

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                // VNPAY kết thúc luồng (đã xử lý rồi)
                result.put("RspCode", "02");
                result.put("Message", "Order already confirmed");
                return result;
            }


            String responseCode = vnpParams.get("vnp_ResponseCode");
            String transactionStatus = vnpParams.get("vnp_TransactionStatus");
            boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

            saveTransactionLog(order, vnpParams, success);
            if (success) {
                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);
            }

            // VNPAY kết thúc luồng
            result.put("RspCode", "00");
            result.put("Message", "Confirm Success");
        } catch (Exception e) {
            log.error("VNPAY IPN processing error", e);
            // VNPAY retry
            result.put("RspCode", "99");
            result.put("Message", "Unknown error");
        }
        return result;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()
                || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Nếu có nhiều IP (proxy chain), lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error while generating HMAC-SHA512", e);
        }
    }

    private void saveTransactionLog(Order order, Map<String, String> vnpParams, boolean success) {
        // vnp_Amount từ VNPAY đã nhân 100, cần chia lại
        BigDecimal actualAmount = BigDecimal.valueOf(
                        Long.parseLong(vnpParams.getOrDefault("vnp_Amount", "0")))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        TransactionLog txLog = TransactionLog.builder()
                .order(order)
                .vnpTmnCode(vnpParams.get("vnp_TmnCode"))
                .vnpTxnRef(vnpParams.get("vnp_TxnRef"))
                .vnpTransactionNo(vnpParams.get("vnp_TransactionNo"))
                .vnpResponseCode(vnpParams.get("vnp_ResponseCode"))
                .vnpTransactionStatus(
                        vnpParams.get("vnp_TransactionStatus"))
                .amount(actualAmount)
                .bankCode(vnpParams.get("vnp_BankCode"))
                .bankTranNo(vnpParams.get("vnp_BankTranNo"))
                .cardType(vnpParams.get("vnp_CardType"))
                .orderInfo(vnpParams.get("vnp_OrderInfo"))
                .payDate(vnpParams.get("vnp_PayDate"))
                .success(success)
                .rawData(vnpParams.toString())
                .build();

        transactionLogRepository.save(txLog);
    }

    private Map<String, String> extractVnpParams(
            HttpServletRequest request) {
        Map<String, String> vnpParams = new TreeMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramValue != null && !paramValue.isEmpty()) {
                vnpParams.put(paramName, paramValue);
            }
        }
        return vnpParams;
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator =
                params.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            sb.append(entry.getKey())
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(),
                            StandardCharsets.US_ASCII));
            if (iterator.hasNext()) {
                sb.append('&');
            }
        }
        return sb.toString();
    }
}
