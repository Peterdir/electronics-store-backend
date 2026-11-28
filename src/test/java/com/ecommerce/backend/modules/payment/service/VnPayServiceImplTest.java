package com.ecommerce.backend.modules.payment.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.payment.config.VnPayConfig;
import com.ecommerce.backend.modules.payment.entity.TransactionLog;
import com.ecommerce.backend.modules.payment.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VnPayServiceImpl Unit Test Suite")
class VnPayServiceImplTest {

    private static final String TMN_CODE = "TESTTMN01";
    private static final String HASH_SECRET = "SECRET_KEY_FOR_VNPAY_UNIT_TEST_123456";
    private static final String PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String RETURN_URL = "http://localhost:3000/payment/return";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    private VnPayConfig vnPayConfig;
    private VnPayServiceImpl vnPayService;
    private Order unpaidOrder;

    @BeforeEach
    void setUp() {
        vnPayConfig = new VnPayConfig();
        vnPayConfig.setTmnCode(TMN_CODE);
        vnPayConfig.setHashSecret(HASH_SECRET);
        vnPayConfig.setPayUrl(PAY_URL);
        vnPayConfig.setReturnUrl(RETURN_URL);

        vnPayService = new VnPayServiceImpl(vnPayConfig, orderRepository, transactionLogRepository);

        unpaidOrder = Order.builder()
                .id(12345L)
                .totalPrice(new BigDecimal("250000"))
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
    }

    private MockHttpServletRequest buildSignedCallbackRequest(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = sorted.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            hashData.append(entry.getKey())
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            if (it.hasNext()) {
                hashData.append('&');
            }
        }
        String secureHash = hmacSha512(HASH_SECRET, hashData.toString());

        MockHttpServletRequest request = new MockHttpServletRequest();
        sorted.forEach(request::addParameter);
        request.addParameter("vnp_SecureHash", secureHash);
        request.addParameter("vnp_SecureHashType", "HMACSHA512");
        return request;
    }

    private Map<String, String> baseValidParams(String responseCode, String txStatus) {
        return Map.ofEntries(
                Map.entry("vnp_TmnCode", TMN_CODE),
                Map.entry("vnp_TxnRef", "12345"),
                Map.entry("vnp_Amount", "25000000"),
                Map.entry("vnp_ResponseCode", responseCode),
                Map.entry("vnp_TransactionStatus", txStatus),
                Map.entry("vnp_TransactionNo", "14422334"),
                Map.entry("vnp_BankCode", "NCB"),
                Map.entry("vnp_BankTranNo", "VNP14422334"),
                Map.entry("vnp_CardType", "ATM"),
                Map.entry("vnp_OrderInfo", "Thanh toan don hang 12345"),
                Map.entry("vnp_PayDate", "20261127150000")
        );
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Create Payment URL Tests")
    class CreatePaymentUrlTests {

        @Test
        @DisplayName("Should generate valid signed VNPay URL with multiplied amount and first X-Forwarded-For IP")
        void createPaymentUrl_WithProxyIp_ReturnsSignedUrl() {
            when(orderRepository.findById(12345L)).thenReturn(Optional.of(unpaidOrder));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

            String url = vnPayService.createPaymentUrl(12345L, request);

            assertThat(url).startsWith(PAY_URL + "?");
            assertThat(url).contains("vnp_TmnCode=" + TMN_CODE);
            assertThat(url).contains("vnp_TxnRef=12345");
            assertThat(url).contains("vnp_Amount=25000000");
            assertThat(url).contains("vnp_IpAddr=203.0.113.10");
            assertThat(url).contains("vnp_CurrCode=VND");
            assertThat(url).contains("vnp_SecureHash=");

            // Kiểm tra chữ ký HMAC-SHA512 khớp với toàn bộ query trước vnp_SecureHash
            String query = url.substring((PAY_URL + "?").length());
            int hashIdx = query.indexOf("&vnp_SecureHash=");
            String rawHashData = query.substring(0, hashIdx);
            String actualHash = query.substring(hashIdx + "&vnp_SecureHash=".length());
            assertEquals(128, actualHash.length());
            assertEquals(hmacSha512(HASH_SECRET, rawHashData), actualHash);
        }

        @Test
        @DisplayName("Should fallback to remoteAddr when X-Forwarded-For is unknown")
        void createPaymentUrl_UnknownForwardedFor_UsesRemoteAddr() {
            when(orderRepository.findById(12345L)).thenReturn(Optional.of(unpaidOrder));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "unknown");
            request.setRemoteAddr("192.168.1.50");

            String url = vnPayService.createPaymentUrl(12345L, request);

            assertThat(url).contains("vnp_IpAddr=192.168.1.50");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order does not exist")
        void createPaymentUrl_OrderNotFound_ThrowsNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());
            MockHttpServletRequest request = new MockHttpServletRequest();

            assertThrows(ResourceNotFoundException.class, () -> vnPayService.createPaymentUrl(999L, request));
        }

        @Test
        @DisplayName("Should throw BadRequestException when order is already PAID")
        void createPaymentUrl_AlreadyPaid_ThrowsBadRequest() {
            unpaidOrder.setPaymentStatus(PaymentStatus.PAID);
            when(orderRepository.findById(12345L)).thenReturn(Optional.of(unpaidOrder));
            MockHttpServletRequest request = new MockHttpServletRequest();

            assertThrows(BadRequestException.class, () -> vnPayService.createPaymentUrl(12345L, request));
        }
    }

    @Nested
    @DisplayName("Handle Return URL Tests")
    class HandleReturnUrlTests {

        @Test
        @DisplayName("Should mark order PAID, save TransactionLog, and return success=true on 00/00")
        void handleReturnUrl_PaymentSuccess_UpdatesOrderAndLogsTransaction() {
            MockHttpServletRequest request = buildSignedCallbackRequest(baseValidParams("00", "00"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));
            when(transactionLogRepository.existsByVnpTxnRefAndVnpTransactionNoAndVnpResponseCodeAndPayDate(
                    anyString(), anyString(), anyString(), anyString())).thenReturn(false);

            Map<String, Object> result = vnPayService.handleReturnUrl(request);

            assertEquals(true, result.get("success"));
            assertEquals("Payment successful!", result.get("message"));
            assertEquals("12345", result.get("orderId"));
            assertEquals(PaymentStatus.PAID, unpaidOrder.getPaymentStatus());
            verify(orderRepository).save(unpaidOrder);

            ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(logCaptor.capture());
            TransactionLog savedLog = logCaptor.getValue();
            assertEquals(new BigDecimal("250000"), savedLog.getAmount());
            assertTrue(savedLog.getSuccess());
            assertEquals("NCB", savedLog.getBankCode());
            assertEquals("14422334", savedLog.getVnpTransactionNo());
        }

        @Test
        @DisplayName("Should return cancelled message and log failed transaction when ResponseCode is 24")
        void handleReturnUrl_CustomerCancelled_LogsFailureAndKeepsUnpaid() {
            MockHttpServletRequest request = buildSignedCallbackRequest(baseValidParams("24", "02"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));
            when(transactionLogRepository.existsByVnpTxnRefAndVnpTransactionNoAndVnpResponseCodeAndPayDate(
                    anyString(), anyString(), anyString(), anyString())).thenReturn(false);

            Map<String, Object> result = vnPayService.handleReturnUrl(request);

            assertEquals(false, result.get("success"));
            assertThat(result.get("message").toString()).contains("Payment cancelled");
            assertEquals(PaymentStatus.UNPAID, unpaidOrder.getPaymentStatus());
            verify(orderRepository, never()).save(any());

            ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(transactionLogRepository).save(logCaptor.capture());
            assertFalse(logCaptor.getValue().getSuccess());
        }

        @Test
        @DisplayName("Should return failed message when ResponseCode is 51")
        void handleReturnUrl_PaymentFailed_LogsFailure() {
            MockHttpServletRequest request = buildSignedCallbackRequest(baseValidParams("51", "02"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));
            when(transactionLogRepository.existsByVnpTxnRefAndVnpTransactionNoAndVnpResponseCodeAndPayDate(
                    anyString(), anyString(), anyString(), anyString())).thenReturn(false);

            Map<String, Object> result = vnPayService.handleReturnUrl(request);

            assertEquals(false, result.get("success"));
            assertThat(result.get("message").toString()).contains("Payment failed");
            verify(orderRepository, never()).save(any());
            verify(transactionLogRepository).save(any(TransactionLog.class));
        }

        @Test
        @DisplayName("Should be idempotent when order is already PAID or transaction log already exists")
        void handleReturnUrl_Idempotency_DoesNotDuplicateLog() {
            // Case 1: Order đã PAID (IPN xử lý trước Return URL)
            unpaidOrder.setPaymentStatus(PaymentStatus.PAID);
            MockHttpServletRequest request = buildSignedCallbackRequest(baseValidParams("00", "00"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));

            Map<String, Object> paidResult = vnPayService.handleReturnUrl(request);
            assertEquals(true, paidResult.get("success"));
            verify(transactionLogRepository, never()).save(any());
            verify(orderRepository, never()).save(any());

            // Case 2: Order chưa PAID nhưng log đã tồn tại
            unpaidOrder.setPaymentStatus(PaymentStatus.UNPAID);
            when(transactionLogRepository.existsByVnpTxnRefAndVnpTransactionNoAndVnpResponseCodeAndPayDate(
                    anyString(), anyString(), anyString(), anyString())).thenReturn(true);

            Map<String, Object> dupLogResult = vnPayService.handleReturnUrl(request);
            assertEquals(true, dupLogResult.get("success"));
            verify(transactionLogRepository, never()).save(any());
            verify(orderRepository).save(unpaidOrder);
        }

        @Test
        @DisplayName("Should fail verification on invalid signature, wrong merchant, bad numbers, missing order, or wrong amount")
        void handleReturnUrl_VerificationFailures_ReturnFalseWithoutSideEffects() {
            // 1. Sai chữ ký
            MockHttpServletRequest badSig = buildSignedCallbackRequest(baseValidParams("00", "00"));
            badSig.setParameter("vnp_SecureHash", "deadbeef");
            assertEquals(false, vnPayService.handleReturnUrl(badSig).get("success"));

            // 2. Sai mã merchant
            Map<String, String> badMerchantParams = new TreeMap<>(baseValidParams("00", "00"));
            badMerchantParams.put("vnp_TmnCode", "WRONG_TMN");
            assertEquals(false, vnPayService.handleReturnUrl(buildSignedCallbackRequest(badMerchantParams)).get("success"));

            // 3. TxnRef không phải số
            Map<String, String> badNumberParams = new TreeMap<>(baseValidParams("00", "00"));
            badNumberParams.put("vnp_TxnRef", "ABC");
            assertEquals(false, vnPayService.handleReturnUrl(buildSignedCallbackRequest(badNumberParams)).get("success"));

            // 4. Không tìm thấy đơn hàng
            MockHttpServletRequest missingOrderReq = buildSignedCallbackRequest(baseValidParams("00", "00"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.empty());
            Map<String, Object> notFoundRes = vnPayService.handleReturnUrl(missingOrderReq);
            assertEquals(false, notFoundRes.get("success"));
            assertEquals("Order not found.", notFoundRes.get("message"));

            // 5. Sai số tiền thanh toán
            Map<String, String> wrongAmtParams = new TreeMap<>(baseValidParams("00", "00"));
            wrongAmtParams.put("vnp_Amount", "1000000");
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));
            assertEquals(false, vnPayService.handleReturnUrl(buildSignedCallbackRequest(wrongAmtParams)).get("success"));

            verifyNoInteractions(transactionLogRepository);
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Handle IPN Webhook Tests")
    class HandleIpnTests {

        @Test
        @DisplayName("Should return RspCode 00 on valid IPN payment confirmation")
        void handleIpn_ValidSuccess_Returns00() {
            MockHttpServletRequest request = buildSignedCallbackRequest(baseValidParams("00", "00"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));
            when(transactionLogRepository.existsByVnpTxnRefAndVnpTransactionNoAndVnpResponseCodeAndPayDate(
                    anyString(), anyString(), anyString(), anyString())).thenReturn(false);

            Map<String, String> res = vnPayService.handleIpn(request);

            assertEquals("00", res.get("RspCode"));
            assertEquals("Confirm Success", res.get("Message"));
            assertEquals(PaymentStatus.PAID, unpaidOrder.getPaymentStatus());
            verify(orderRepository).save(unpaidOrder);
            verify(transactionLogRepository).save(any(TransactionLog.class));
        }

        @Test
        @DisplayName("Should return RspCode 00 and keep UNPAID when IPN reports failed payment")
        void handleIpn_ValidFailureCode_Returns00AndKeepsUnpaid() {
            MockHttpServletRequest request = buildSignedCallbackRequest(baseValidParams("51", "02"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));
            when(transactionLogRepository.existsByVnpTxnRefAndVnpTransactionNoAndVnpResponseCodeAndPayDate(
                    anyString(), anyString(), anyString(), anyString())).thenReturn(false);

            Map<String, String> res = vnPayService.handleIpn(request);

            assertEquals("00", res.get("RspCode"));
            assertEquals(PaymentStatus.UNPAID, unpaidOrder.getPaymentStatus());
            verify(orderRepository, never()).save(any());
            verify(transactionLogRepository).save(any(TransactionLog.class));
        }

        @Test
        @DisplayName("Should return RspCode 02 when order is already PAID")
        void handleIpn_AlreadyPaid_Returns02() {
            unpaidOrder.setPaymentStatus(PaymentStatus.PAID);
            MockHttpServletRequest request = buildSignedCallbackRequest(baseValidParams("00", "00"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));

            Map<String, String> res = vnPayService.handleIpn(request);

            assertEquals("02", res.get("RspCode"));
            assertEquals("Order already confirmed", res.get("Message"));
            verifyNoInteractions(transactionLogRepository);
        }

        @Test
        @DisplayName("Should return exact VNPay error RspCodes for checksum, merchant, order, amount, and exceptions")
        void handleIpn_ErrorCodes_MatchVnPaySpec() {
            // 97 - Invalid Checksum
            MockHttpServletRequest badSig = buildSignedCallbackRequest(baseValidParams("00", "00"));
            badSig.setParameter("vnp_SecureHash", "wrong_hash");
            assertEquals("97", vnPayService.handleIpn(badSig).get("RspCode"));

            // 99 - Invalid Merchant
            Map<String, String> badMerchant = new TreeMap<>(baseValidParams("00", "00"));
            badMerchant.put("vnp_TmnCode", "OTHER");
            assertEquals("99", vnPayService.handleIpn(buildSignedCallbackRequest(badMerchant)).get("RspCode"));

            // 99 - Invalid Request (non-numeric amount)
            Map<String, String> badNumber = new TreeMap<>(baseValidParams("00", "00"));
            badNumber.put("vnp_Amount", "NAN");
            assertEquals("99", vnPayService.handleIpn(buildSignedCallbackRequest(badNumber)).get("RspCode"));

            // 01 - Order Not Found
            MockHttpServletRequest notFoundReq = buildSignedCallbackRequest(baseValidParams("00", "00"));
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.empty());
            assertEquals("01", vnPayService.handleIpn(notFoundReq).get("RspCode"));

            // 04 - Invalid Amount
            Map<String, String> wrongAmount = new TreeMap<>(baseValidParams("00", "00"));
            wrongAmount.put("vnp_Amount", "999");
            when(orderRepository.findByIdForUpdate(12345L)).thenReturn(Optional.of(unpaidOrder));
            assertEquals("04", vnPayService.handleIpn(buildSignedCallbackRequest(wrongAmount)).get("RspCode"));

            // 99 - Unexpected Exception
            MockHttpServletRequest exceptionReq = buildSignedCallbackRequest(baseValidParams("00", "00"));
            when(orderRepository.findByIdForUpdate(12345L)).thenThrow(new RuntimeException("DB lock timeout"));
            assertEquals("99", vnPayService.handleIpn(exceptionReq).get("RspCode"));
        }
    }
}
