package com.ecommerce.backend.modules.returnrequest.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderItemRepository;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestProcessRequest;
import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestCreateRequest;
import com.ecommerce.backend.modules.returnrequest.entity.ReturnRequest;
import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import com.ecommerce.backend.modules.returnrequest.mapper.ReturnRequestMapper;
import com.ecommerce.backend.modules.returnrequest.repository.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceImplTest {
    @Mock private ReturnRequestRepository returnRequestRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Spy private ReturnRequestMapper returnRequestMapper = new ReturnRequestMapper();
    @InjectMocks private ReturnRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(10L).user(user)
                .orderStatus(OrderStatus.DELIVERED).updatedAt(Instant.now()).build();
        OrderItem item = OrderItem.builder().id(100L).order(order).quantity(2).build();
        lenient().when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(orderItemRepository.findById(100L)).thenReturn(Optional.of(item));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DELIVERED", "RETURN_REQUESTED", "RETURNED", "RETURN_REJECTED"})
    void submittingAnotherItemPreservesDeadlineAndSetsRequested(String status) {
        Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
        order.setOrderStatus(OrderStatus.valueOf(status));
        Instant deadline = Instant.now().plusSeconds(60);
        order.setReturnDeadline(deadline);
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service.createReturnRequest(1L, request("Lý do khác", null));
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
        assertThat(order.getReturnDeadline()).isEqualTo(deadline);
    }

    @Test
    void updatedTimestampDoesNotExtendExpiredDeadline() {
        Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnDeadline(Instant.now().minusSeconds(1));
        order.setUpdatedAt(Instant.now());
        assertThrows(BadRequestException.class,
                () -> service.createReturnRequest(1L, request("Lý do khác", null)));
        verify(returnRequestRepository, never()).save(any());
    }

    @Test
    void firstRequestFreezesDeadlineAndLegacyRequestsHaveEffectiveStatus() {
        Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
        Instant expected = order.getUpdatedAt().plusSeconds(7 * 24 * 60 * 60);
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service.createReturnRequest(1L, request("Lý do khác", null));
        order.setUpdatedAt(Instant.now().plusSeconds(300));
        order.setOrderStatus(OrderStatus.DELIVERED);
        assertThat(order.getReturnDeadline()).isEqualTo(expected);
        assertThat(order.getEffectiveOrderStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    void rejectingOneRequestKeepsOrderPendingUntilAllRequestsAreProcessed() {
        Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        ReturnRequest first = ReturnRequest.builder().id(200L).order(order).status(ReturnStatus.PENDING).build();
        ReturnRequest second = ReturnRequest.builder().id(201L).order(order).status(ReturnStatus.PENDING).build();
        order.getReturnRequests().addAll(List.of(first, second));
        when(returnRequestRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(first));
        when(returnRequestRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(second));
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ReturnRequestProcessRequest decision = new ReturnRequestProcessRequest();
        decision.setStatus(ReturnStatus.REJECTED);
        decision.setRejectReason("Outside policy");
        service.processReturnRequest(200L, decision);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
        service.processReturnRequest(201L, decision);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.RETURN_REJECTED);
        assertThrows(BadRequestException.class, () -> service.processReturnRequest(201L, decision));
    }

    @Test
    void ApprovedRequestTakesPriorityOverRejectedRequests() {
        Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
        order.getReturnRequests().add(ReturnRequest.builder().status(ReturnStatus.REJECTED).build());
        order.getReturnRequests().add(ReturnRequest.builder().status(ReturnStatus.APPROVED).build());
        assertThat(order.getEffectiveOrderStatus()).isEqualTo(OrderStatus.RETURNED);
        order.getReturnRequests().add(ReturnRequest.builder().status(ReturnStatus.PENDING).build());
        assertThat(order.getEffectiveOrderStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    private ReturnRequestCreateRequest request(String reason, List<MultipartFile> images) {
        ReturnRequestCreateRequest request = new ReturnRequestCreateRequest();
        request.setOrderItemId(100L);
        request.setQuantity(1);
        request.setReason(reason);
        request.setImages(images);
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Sản phẩm bị lỗi hoặc không hoạt động",
            "Sản phẩm bị hư hỏng, móp méo hoặc vỡ"
    })
    void faultyOrDamagedProductsRequireImages(String reason) {
        assertThrows(BadRequestException.class,
                () -> service.createReturnRequest(1L, request(" " + reason + " ", null)));
        assertThrows(BadRequestException.class,
                () -> service.createReturnRequest(1L, request(reason, List.of())));
        verify(returnRequestRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Giao sai sản phẩm hoặc phiên bản",
            "Sản phẩm không đúng mô tả",
            "Thiếu phụ kiện hoặc bộ phận đi kèm",
            "Sản phẩm có dấu hiệu đã qua sử dụng",
            "Tôi không còn nhu cầu sử dụng"
    })
    void otherReasonsAcceptMissingOrEmptyImages(String reason) {
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var withoutPart = service.createReturnRequest(1L, request(" " + reason + " ", null));
        var emptyList = service.createReturnRequest(1L, request(reason, List.of()));
        assertThat(withoutPart.getReason()).isEqualTo(reason);
        assertThat(withoutPart.getImageUrls()).isEmpty();
        assertThat(emptyList.getImageUrls()).isEmpty();
        assertThat(withoutPart.getStatus()).isEqualTo(ReturnStatus.PENDING);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void optionalUploadsStillRejectInvalidFilesAndTooManyImages() {
        MultipartFile invalid = new MockMultipartFile("images", "bad.gif", "image/gif", new byte[]{1});
        assertThrows(BadRequestException.class,
                () -> service.createReturnRequest(1L, request("Lý do khác", List.of(invalid))));
        assertThrows(BadRequestException.class,
                () -> service.createReturnRequest(1L, request("Lý do khác", Collections.nCopies(6, invalid))));
        verify(returnRequestRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Sản phẩm bị lỗi hoặc không hoạt động", "Tôi không còn nhu cầu sử dụng"})
    void validImagesAreUploadedForBothRequiredAndOptionalReasons(String reason) {
        MultipartFile image = new MockMultipartFile("images", "proof.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        when(fileStorageService.uploadFile(image, "returns")).thenReturn("/uploads/returns/proof.png");
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.createReturnRequest(1L, request(reason, List.of(image)));
        assertThat(response.getImageUrls()).containsExactly("/uploads/returns/proof.png");
    }
}
