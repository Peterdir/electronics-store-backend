package com.ecommerce.backend.modules.returnrequest.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderItemRepository;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestProcessRequest;
import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestCreateRequest;
import com.ecommerce.backend.modules.returnrequest.dto.response.ReturnRequestResponse;
import com.ecommerce.backend.modules.returnrequest.entity.ReturnRequest;
import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import com.ecommerce.backend.modules.returnrequest.mapper.ReturnRequestMapper;
import com.ecommerce.backend.modules.returnrequest.repository.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
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

    @Nested
    @DisplayName("Create Return Request Validation Tests")
    class CreateReturnRequestValidationTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user, orderItem, or order is missing")
        void createReturnRequest_MissingEntities_ThrowsResourceNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> service.createReturnRequest(99L, request("Lý do khác", null)));

            when(orderItemRepository.findById(999L)).thenReturn(Optional.empty());
            ReturnRequestCreateRequest missingItemReq = request("Lý do khác", null);
            missingItemReq.setOrderItemId(999L);
            assertThrows(ResourceNotFoundException.class,
                    () -> service.createReturnRequest(1L, missingItemReq));

            when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> service.createReturnRequest(1L, request("Lý do khác", null)));
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when order user does not match authenticated user")
        void createReturnRequest_OrderOwnedByOtherUser_ThrowsAccessDenied() {
            User another = User.builder().id(2L).build();
            when(userRepository.findById(2L)).thenReturn(Optional.of(another));
            assertThrows(AccessDeniedException.class,
                    () -> service.createReturnRequest(2L, request("Lý do khác", null)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"PENDING", "PROCESSING", "SHIPPED", "CANCELLED"})
        @DisplayName("Should reject return request when order has not been delivered")
        void createReturnRequest_UndeliveredOrder_ThrowsBadRequest(String status) {
            Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
            order.setOrderStatus(OrderStatus.valueOf(status));

            assertThrows(BadRequestException.class,
                    () -> service.createReturnRequest(1L, request("Lý do khác", null)));
            verify(returnRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject when order status is null, quantity exceeds purchased, or duplicate exists")
        void createReturnRequest_InvalidQuantityOrDuplicate_ThrowsExpectedExceptions() {
            Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
            order.setOrderStatus(null);
            assertThrows(BadRequestException.class,
                    () -> service.createReturnRequest(1L, request("Lý do khác", null)));

            order.setOrderStatus(OrderStatus.DELIVERED);
            ReturnRequestCreateRequest overQty = request("Lý do khác", null);
            overQty.setQuantity(5);
            assertThrows(BadRequestException.class,
                    () -> service.createReturnRequest(1L, overQty));

            when(returnRequestRepository.existsByOrderItemId(100L)).thenReturn(true);
            assertThrows(DuplicateResourceException.class,
                    () -> service.createReturnRequest(1L, request("Lý do khác", null)));
        }
    }

    @Nested
    @DisplayName("Admin Get & Process Return Request Tests")
    class AdminReturnRequestTests {

        @Test
        @DisplayName("Should query return requests by status with descending createdAt sort")
        void getReturnRequestsByStatus_ReturnsMappedPage() {
            Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
            OrderItem item = orderItemRepository.findById(100L).orElseThrow();
            ReturnRequest entity = ReturnRequest.builder()
                    .id(500L)
                    .order(order)
                    .orderItem(item)
                    .quantity(1)
                    .reason("Lý do khác")
                    .status(ReturnStatus.PENDING)
                    .build();

            PageRequest expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            when(returnRequestRepository.findByStatus(ReturnStatus.PENDING, expectedPageable))
                    .thenReturn(new PageImpl<>(List.of(entity), expectedPageable, 1));

            Page<ReturnRequestResponse> page = service.getReturnRequestsByStatus(ReturnStatus.PENDING, 0, 10);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(500L);
            assertThat(page.getContent().get(0).getStatus()).isEqualTo(ReturnStatus.PENDING);
        }

        @Test
        @DisplayName("Should approve return request, restock inventory, and mark order RETURNED")
        void processReturnRequest_Approve_RestocksInventoryAndUpdatesOrder() {
            Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
            order.setOrderStatus(OrderStatus.RETURN_REQUESTED);

            Inventory inventory = Inventory.builder().id(300L).quantity(5L).build();
            ProductVariant variant = ProductVariant.builder().id(400L).inventory(inventory).build();
            OrderItem item = orderItemRepository.findById(100L).orElseThrow();
            item.setProductVariant(variant);

            ReturnRequest req = ReturnRequest.builder()
                    .id(200L)
                    .order(order)
                    .orderItem(item)
                    .quantity(2)
                    .status(ReturnStatus.PENDING)
                    .build();
            order.getReturnRequests().add(req);

            when(returnRequestRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(req));
            when(returnRequestRepository.save(any(ReturnRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ReturnRequestProcessRequest decision = new ReturnRequestProcessRequest();
            decision.setStatus(ReturnStatus.APPROVED);

            ReturnRequestResponse response = service.processReturnRequest(200L, decision);

            assertThat(response.getStatus()).isEqualTo(ReturnStatus.APPROVED);
            assertThat(inventory.getQuantity()).isEqualTo(7L);
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.RETURNED);
        }

        @Test
        @DisplayName("Should throw expected exceptions for missing entities, blank rejectReason, or invalid status")
        void processReturnRequest_InvalidCases_ThrowExpectedExceptions() {
            when(returnRequestRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());
            ReturnRequestProcessRequest approve = new ReturnRequestProcessRequest();
            approve.setStatus(ReturnStatus.APPROVED);
            assertThrows(ResourceNotFoundException.class, () -> service.processReturnRequest(999L, approve));

            Order order = orderRepository.findByIdForUpdate(10L).orElseThrow();
            ReturnRequest pending = ReturnRequest.builder()
                    .id(200L)
                    .order(order)
                    .status(ReturnStatus.PENDING)
                    .build();
            when(returnRequestRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(pending));

            // Order không tìm thấy
            when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.processReturnRequest(200L, approve));

            // Từ chối thiếu lý do
            when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
            ReturnRequestProcessRequest blankReject = new ReturnRequestProcessRequest();
            blankReject.setStatus(ReturnStatus.REJECTED);
            blankReject.setRejectReason("   ");
            assertThrows(BadRequestException.class, () -> service.processReturnRequest(200L, blankReject));

            // Trạng thái không hợp lệ (PENDING hoặc null)
            ReturnRequestProcessRequest invalidStatus = new ReturnRequestProcessRequest();
            invalidStatus.setStatus(ReturnStatus.PENDING);
            assertThrows(BadRequestException.class, () -> service.processReturnRequest(200L, invalidStatus));
        }
    }
}
