package com.ecommerce.backend.modules.payment.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.order.entity.Order;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {

    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String vnpTmnCode;       // Mã website

    private String vnpTxnRef;        // Mã giao dịch do hệ thống ecommerce tạo ra

    private String vnpTransactionNo; // Mã giao dịch do VNPAY cung cấp

    private String vnpResponseCode;  // Mã kết quả: "00" = thành công

    private String vnpTransactionStatus; // Trạng thái GD VNPAY v2.1.0

    private BigDecimal amount;       // Số tiền thực (VND, đã chia 100)

    private String bankCode;         // Mã ngân hàng: NCB, VISA...

    private String bankTranNo;       // Mã GD tại ngân hàng

    private String cardType;         // ATM, QRCODE

    private String orderInfo;        // Nội dung thanh toán

    private String payDate;          // Thời điểm thanh toán do VNPAY trả về

    @Builder.Default
    private Boolean success = false;

    @Column(columnDefinition = "TEXT")
    private String rawData;          // Toàn bộ query string trả về

    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
