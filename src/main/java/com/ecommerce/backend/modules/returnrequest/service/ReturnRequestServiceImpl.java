package com.ecommerce.backend.modules.returnrequest.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.common.utils.FileValidator;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderItemRepository;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestCreateRequest;
import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestProcessRequest;
import com.ecommerce.backend.modules.returnrequest.dto.response.ReturnRequestResponse;
import com.ecommerce.backend.modules.returnrequest.entity.ReturnRequest;
import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import com.ecommerce.backend.modules.returnrequest.mapper.ReturnRequestMapper;
import com.ecommerce.backend.modules.returnrequest.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ReturnRequestMapper returnRequestMapper;

    @Override
    public ReturnRequestResponse createReturnRequest(Long userId, ReturnRequestCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        Order order = orderItem.getOrder();

        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this order item.");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Only DELIVERED orders can be returned.");
        }

        Instant returnDeadline = order.getUpdatedAt().plus(7, ChronoUnit.DAYS);
        if (Instant.now().isAfter(returnDeadline)) {
            throw new BadRequestException("The return period (7 days) for this item has expired.");
        }

        if (request.getQuantity() > orderItem.getQuantity()) {
            throw new BadRequestException("Return quantity cannot exceed the purchased quantity (" + orderItem.getQuantity() + ").");
        }

        if (returnRequestRepository.existsByOrderItemId(orderItem.getId())) {
            throw new DuplicateResourceException("A return request already exists for this order item.");
        }

        List<String> uploadedImageUrls = new ArrayList<>();
        List<MultipartFile> images = request.getImages();

        if (images == null || images.isEmpty()) {
            throw new BadRequestException("Please upload at least one image as proof of the issue.");
        }

        if (images.size() > 5) {
            throw new BadRequestException("You can upload a maximum of 5 images.");
        }

        for (MultipartFile file : images) {
            FileValidator.validateImageFile(file);
            String url = fileStorageService.uploadFile(file, "returns");
            uploadedImageUrls.add(url);
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .user(user)
                .order(order)
                .orderItem(orderItem)
                .quantity(request.getQuantity())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReturnStatus.PENDING)
                .imageUrls(uploadedImageUrls)
                .build();

        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

        return returnRequestMapper.toResponse(savedRequest);
    }

    @Override
    public Page<ReturnRequestResponse> getReturnRequestsByStatus(ReturnStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ReturnRequest> returnRequests = returnRequestRepository.findByStatus(status, pageable);

        return returnRequests.map(returnRequestMapper::toResponse);
    }

    @Override
    public ReturnRequestResponse processReturnRequest(Long id, ReturnRequestProcessRequest request) {
        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        if (returnRequest.getStatus() != ReturnStatus.PENDING) {
            throw new BadRequestException("This return request has already been processed or cancelled.");
        }

        Order order = returnRequest.getOrder();

        if (request.getStatus() == ReturnStatus.APPROVED) {
            returnRequest.setStatus(ReturnStatus.APPROVED);
            order.setOrderStatus(OrderStatus.RETURNED);

            ProductVariant variant = returnRequest.getOrderItem().getProductVariant();
            Inventory inventory = variant.getInventory();
            inventory.setQuantity(inventory.getQuantity() + returnRequest.getQuantity());

        } else if (request.getStatus() == ReturnStatus.REJECTED) {
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new BadRequestException("Please provide a reason for rejecting the return request.");
            }
            returnRequest.setStatus(ReturnStatus.REJECTED);
            returnRequest.setAdminNote(request.getRejectReason());
            order.setOrderStatus(OrderStatus.RETURN_REJECTED);
        } else {
             throw new BadRequestException("Invalid return status in request.");
        }

        return returnRequestMapper.toResponse(returnRequestRepository.save(returnRequest));
    }
}
