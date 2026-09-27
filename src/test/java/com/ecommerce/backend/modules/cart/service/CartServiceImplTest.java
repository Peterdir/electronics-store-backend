package com.ecommerce.backend.modules.cart.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.cart.dto.request.AddToCartRequest;
import com.ecommerce.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.ecommerce.backend.modules.cart.dto.response.CartCheckoutValidationResponse;
import com.ecommerce.backend.modules.cart.dto.response.CartItemIssueResponse;
import com.ecommerce.backend.modules.cart.dto.response.CartResponse;
import com.ecommerce.backend.modules.cart.entity.Cart;
import com.ecommerce.backend.modules.cart.entity.CartItem;
import com.ecommerce.backend.modules.cart.repository.CartItemRepository;
import com.ecommerce.backend.modules.cart.repository.CartRepository;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Unit Test Suite")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User testUser;
    private Cart testCart;
    private Product activeProduct;
    private ProductVariant activeVariant;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100L);
        testUser.setEmail("buyer@example.com");

        testCart = Cart.builder()
                .id(1L)
                .user(testUser)
                .items(new ArrayList<>())
                .build();

        activeProduct = Product.builder()
                .id(200L)
                .name("MacBook Pro M3")
                .status(ProductStatus.ACTIVE)
                .basePrice(BigDecimal.valueOf(2000.00))
                .build();

        testInventory = Inventory.builder()
                .id(400L)
                .quantity(10L)
                .build();

        activeVariant = ProductVariant.builder()
                .id(300L)
                .sku("MBP-M3-16GB")
                .price(BigDecimal.valueOf(2200.00))
                .status(ProductStatus.ACTIVE)
                .product(activeProduct)
                .inventory(testInventory)
                .attributes(new HashMap<>())
                .build();
    }

    // =========================================================================
    // AC-CART-01: Xem giỏ hàng (getCart)
    // =========================================================================
    @Nested
    @DisplayName("AC-CART-01: Get Cart Tests")
    class GetCartTests {

        @Test
        @DisplayName("TC-CART-01 [Positive]: Get existing cart with items successfully")
        void getCart_ExistingCart_Success() {
            // Arrange
            CartItem cartItem = CartItem.builder()
                    .id(501L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(2)
                    .build();
            testCart.getItems().add(cartItem);

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartResponse response = cartService.getCart(testUser.getId());

            // Assert
            assertNotNull(response);
            assertEquals(testCart.getId(), response.getId());
            assertEquals(1, response.getItems().size());
            assertEquals(2, response.getTotalItems());
            assertEquals(BigDecimal.valueOf(4400.00), response.getTotalPrice());
            assertEquals(501L, response.getItems().get(0).getId());
            assertEquals("MacBook Pro M3", response.getItems().get(0).getProductName());
            assertEquals(10L, response.getItems().get(0).getAvailableStock());
            assertFalse(response.getItems().get(0).getIsOutOfStock());
            assertTrue(response.getItems().get(0).getHasSufficientStock());

            verify(cartRepository, times(1)).findByUserId(testUser.getId());
        }

        @Test
        @DisplayName("TC-CART-02 [Boundary]: User has no cart yet -> automatically create a new empty cart")
        void getCart_NoExistingCart_CreatesNewCart() {
            // Arrange
            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
                Cart saved = invocation.getArgument(0);
                saved.setId(99L);
                return saved;
            });

            // Act
            CartResponse response = cartService.getCart(testUser.getId());

            // Assert
            assertNotNull(response);
            assertEquals(99L, response.getId());
            assertEquals(0, response.getItems().size());
            assertEquals(0, response.getTotalItems());
            assertEquals(BigDecimal.ZERO, response.getTotalPrice());

            verify(userRepository, times(1)).findById(testUser.getId());
            verify(cartRepository, times(1)).save(any(Cart.class));
        }

        @Test
        @DisplayName("TC-CART-03 [Negative]: User does not exist when attempting to get/create cart -> throws ResourceNotFoundException")
        void getCart_UserNotFound_ThrowsException() {
            // Arrange
            when(cartRepository.findByUserId(999L)).thenReturn(Optional.empty());
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                    cartService.getCart(999L)
            );
            assertEquals("Not found user.", ex.getMessage());
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    // =========================================================================
    // AC-CART-02: Thêm sản phẩm vào giỏ (addCart)
    // =========================================================================
    @Nested
    @DisplayName("AC-CART-02: Add To Cart Tests")
    class AddCartTests {

        @Test
        @DisplayName("TC-CART-04 [Negative]: Variant ID does not exist -> throws ResourceNotFoundException")
        void addCart_VariantNotFound_ThrowsException() {
            // Arrange
            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(999L)
                    .quantity(1)
                    .build();
            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                    cartService.addCart(testUser.getId(), request)
            );
            assertEquals("Product variant not found.", ex.getMessage());
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("TC-CART-05 [Negative]: Product is INACTIVE -> throws BadRequestException")
        void addCart_ProductInactive_ThrowsException() {
            // Arrange
            activeProduct.setStatus(ProductStatus.INACTIVE);
            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.addCart(testUser.getId(), request)
            );
            assertEquals("This product is currently not available for purchase.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-CART-06 [Negative]: Product is ACTIVE but Variant is INACTIVE -> throws BadRequestException")
        void addCart_VariantInactive_ThrowsException() {
            // Arrange
            activeVariant.setStatus(ProductStatus.INACTIVE);
            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.addCart(testUser.getId(), request)
            );
            assertEquals("This product is currently not available for purchase.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-CART-07 [Negative]: Adding new item with requested quantity > stock -> throws BadRequestException")
        void addCart_NewItem_ExceedsStock_ThrowsException() {
            // Arrange (Stock is 10, Requesting 11)
            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(11)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.addCart(testUser.getId(), request)
            );
            assertEquals("Only 10 items left in stock.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-CART-08 [Negative]: Adding existing item where (existing + added) > stock -> throws BadRequestException")
        void addCart_ExistingItem_ExceedsStock_ThrowsException() {
            // Arrange (Already 7 in cart, Stock is 10, adding 4 -> 11 > 10)
            CartItem existingItem = CartItem.builder()
                    .id(601L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(7)
                    .build();
            testCart.getItems().add(existingItem);

            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(4)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.addCart(testUser.getId(), request)
            );
            assertEquals("Only 10 items left in stock.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-CART-09 [Positive]: Add new item to empty cart within stock -> successfully saved")
        void addCart_NewItem_Success() {
            // Arrange
            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(3)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CartResponse response = cartService.addCart(testUser.getId(), request);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertEquals(3, response.getTotalItems());
            assertEquals(BigDecimal.valueOf(6600.00), response.getTotalPrice());

            verify(cartRepository, times(1)).save(testCart);
        }

        @Test
        @DisplayName("TC-CART-10 [Positive]: Add item that already exists in cart -> quantity increments")
        void addCart_ExistingItem_Success() {
            // Arrange
            CartItem existingItem = CartItem.builder()
                    .id(701L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(2)
                    .build();
            testCart.getItems().add(existingItem);

            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(3)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CartResponse response = cartService.addCart(testUser.getId(), request);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertEquals(5, response.getTotalItems());
            assertEquals(5, existingItem.getQuantity());

            verify(cartRepository, times(1)).save(testCart);
        }

        @Test
        @DisplayName("TC-CART-11 [Boundary]: Add quantity exactly equal to remaining stock (boundary test)")
        void addCart_ExactStockLimit_Success() {
            // Arrange (Stock = 10, Requesting exact 10)
            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(10)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CartResponse response = cartService.addCart(testUser.getId(), request);

            // Assert
            assertNotNull(response);
            assertEquals(10, response.getTotalItems());
            verify(cartRepository, times(1)).save(testCart);
        }

        @Test
        @DisplayName("TC-CART-12 [Edge Case]: Variant has null inventory / null quantity -> treated as 0 stock")
        void addCart_NullInventoryQuantity_ThrowsException() {
            // Arrange
            activeVariant.setInventory(null);
            AddToCartRequest request = AddToCartRequest.builder()
                    .productVariantId(activeVariant.getId())
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(productVariantRepository.findById(activeVariant.getId())).thenReturn(Optional.of(activeVariant));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.addCart(testUser.getId(), request)
            );
            assertEquals("Only 0 items left in stock.", ex.getMessage());
        }
    }

    // =========================================================================
    // AC-CART-03: Cập nhật số lượng item (updateItemQuantity)
    // =========================================================================
    @Nested
    @DisplayName("AC-CART-03: Update Item Quantity Tests")
    class UpdateItemQuantityTests {

        @Test
        @DisplayName("TC-CART-13 [Negative]: Cart item ID does not exist -> throws ResourceNotFoundException")
        void updateItemQuantity_ItemNotFound_ThrowsException() {
            // Arrange
            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(2);
            when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                    cartService.updateItemQuantity(testUser.getId(), 999L, request)
            );
            assertEquals("Cart item not found.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-CART-14 [Negative]: Cart item belongs to another user -> throws BadRequestException (Security/Unauthorized)")
        void updateItemQuantity_DifferentUser_ThrowsException() {
            // Arrange
            User otherUser = new User();
            otherUser.setId(999L);
            Cart otherCart = Cart.builder().id(2L).user(otherUser).build();

            CartItem cartItem = CartItem.builder()
                    .id(801L)
                    .cart(otherCart)
                    .productVariant(activeVariant)
                    .quantity(1)
                    .build();

            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(2);

            when(cartItemRepository.findById(801L)).thenReturn(Optional.of(cartItem));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.updateItemQuantity(testUser.getId(), 801L, request)
            );
            assertEquals("You don't have permission.", ex.getMessage());
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("TC-CART-15 [Negative]: Update quantity exceeds available stock -> throws BadRequestException")
        void updateItemQuantity_ExceedsStock_ThrowsException() {
            // Arrange (Stock is 10, updating to 12)
            CartItem cartItem = CartItem.builder()
                    .id(802L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(1)
                    .build();

            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(12);

            when(cartItemRepository.findById(802L)).thenReturn(Optional.of(cartItem));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.updateItemQuantity(testUser.getId(), 802L, request)
            );
            assertEquals("Only 10 items left in stock.", ex.getMessage());
            verify(cartItemRepository, never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("TC-CART-16 [Positive]: Update quantity valid -> successfully saved")
        void updateItemQuantity_Success() {
            // Arrange
            CartItem cartItem = CartItem.builder()
                    .id(803L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(1)
                    .build();
            testCart.getItems().add(cartItem);

            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(4);

            when(cartItemRepository.findById(803L)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            CartResponse response = cartService.updateItemQuantity(testUser.getId(), 803L, request);

            // Assert
            assertNotNull(response);
            assertEquals(4, cartItem.getQuantity());
            assertEquals(4, response.getTotalItems());
            verify(cartItemRepository, times(1)).save(cartItem);
        }

        @Test
        @DisplayName("TC-CART-17 [Boundary]: Update quantity to exactly the available stock limit")
        void updateItemQuantity_ExactStockLimit_Success() {
            // Arrange (Stock is 10, updating to exact 10)
            CartItem cartItem = CartItem.builder()
                    .id(804L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(2)
                    .build();
            testCart.getItems().add(cartItem);

            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(10);

            when(cartItemRepository.findById(804L)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            CartResponse response = cartService.updateItemQuantity(testUser.getId(), 804L, request);

            // Assert
            assertNotNull(response);
            assertEquals(10, cartItem.getQuantity());
            verify(cartItemRepository, times(1)).save(cartItem);
        }
    }

    // =========================================================================
    // AC-CART-04: Xóa 1 item khỏi giỏ (removeItem)
    // =========================================================================
    @Nested
    @DisplayName("AC-CART-04: Remove Item Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("TC-CART-18 [Negative]: Cart item ID not found -> throws ResourceNotFoundException")
        void removeItem_NotFound_ThrowsException() {
            // Arrange
            when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                    cartService.removeItem(testUser.getId(), 999L)
            );
            assertEquals("Cart item not found.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-CART-19 [Negative]: Cart item belongs to another user -> throws BadRequestException")
        void removeItem_UnauthorizedUser_ThrowsException() {
            // Arrange
            User otherUser = new User();
            otherUser.setId(888L);
            Cart otherCart = Cart.builder().id(9L).user(otherUser).build();

            CartItem cartItem = CartItem.builder()
                    .id(901L)
                    .cart(otherCart)
                    .productVariant(activeVariant)
                    .build();

            when(cartItemRepository.findById(901L)).thenReturn(Optional.of(cartItem));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    cartService.removeItem(testUser.getId(), 901L)
            );
            assertEquals("You don't have permission.", ex.getMessage());
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("TC-CART-20 [Positive]: Remove existing item from cart -> item removed and cart saved")
        void removeItem_Success() {
            // Arrange
            CartItem cartItem = CartItem.builder()
                    .id(902L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(2)
                    .build();
            testCart.getItems().add(cartItem);

            when(cartItemRepository.findById(902L)).thenReturn(Optional.of(cartItem));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            CartResponse response = cartService.removeItem(testUser.getId(), 902L);

            // Assert
            assertNotNull(response);
            assertEquals(0, response.getItems().size());
            assertEquals(0, response.getTotalItems());
            assertFalse(testCart.getItems().contains(cartItem));
            verify(cartRepository, times(1)).save(testCart);
        }
    }

    // =========================================================================
    // AC-CART-05: Xóa sạch toàn bộ giỏ (clearCart)
    // =========================================================================
    @Nested
    @DisplayName("AC-CART-05: Clear Cart Tests")
    class ClearCartTests {

        @Test
        @DisplayName("TC-CART-21 [Positive]: Clear cart with existing items -> items cleared and saved")
        void clearCart_Success() {
            // Arrange
            CartItem item1 = CartItem.builder().id(11L).cart(testCart).productVariant(activeVariant).quantity(2).build();
            CartItem item2 = CartItem.builder().id(12L).cart(testCart).productVariant(activeVariant).quantity(3).build();
            testCart.getItems().add(item1);
            testCart.getItems().add(item2);

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            MessageResponse response = cartService.clearCart(testUser.getId());

            // Assert
            assertNotNull(response);
            assertEquals("Cart cleared successfully.", response.getMessage());
            assertTrue(testCart.getItems().isEmpty());
            verify(cartRepository, times(1)).save(testCart);
        }

        @Test
        @DisplayName("TC-CART-22 [Boundary]: Clear cart that is already empty -> returns success message")
        void clearCart_AlreadyEmpty_Success() {
            // Arrange
            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            MessageResponse response = cartService.clearCart(testUser.getId());

            // Assert
            assertNotNull(response);
            assertEquals("Cart cleared successfully.", response.getMessage());
            assertTrue(testCart.getItems().isEmpty());
        }
    }

    // =========================================================================
    // AC-CART-06: Xác thực giỏ hàng trước khi đặt hàng (validateCartForCheckout)
    // =========================================================================
    @Nested
    @DisplayName("AC-CART-06: Validate Cart For Checkout Tests")
    class ValidateCartForCheckoutTests {

        @Test
        @DisplayName("TC-CART-23 [Edge Case]: Cart is completely empty -> returns invalid with empty cart message")
        void validateCart_EmptyCart_ReturnsInvalid() {
            // Arrange
            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartCheckoutValidationResponse response = cartService.validateCartForCheckout(testUser.getId());

            // Assert
            assertNotNull(response);
            assertFalse(response.getIsValid());
            assertEquals("Your cart is currently empty.", response.getMessage());
            assertTrue(response.getIssues().isEmpty());
        }

        @Test
        @DisplayName("TC-CART-24 [Positive]: All items in cart are active and within stock -> valid for checkout")
        void validateCart_AllValidItems_ReturnsValid() {
            // Arrange (Stock is 10, item quantity is 5)
            CartItem validItem = CartItem.builder()
                    .id(1001L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(5)
                    .build();
            testCart.getItems().add(validItem);

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartCheckoutValidationResponse response = cartService.validateCartForCheckout(testUser.getId());

            // Assert
            assertNotNull(response);
            assertTrue(response.getIsValid());
            assertEquals("Cart is valid for checkout.", response.getMessage());
            assertTrue(response.getIssues().isEmpty());
        }

        @Test
        @DisplayName("TC-CART-25 [Negative]: Product is INACTIVE -> reports issue 'This item is no longer available.'")
        void validateCart_InactiveProduct_ReportsIssue() {
            // Arrange
            activeProduct.setStatus(ProductStatus.INACTIVE);
            CartItem item = CartItem.builder()
                    .id(1002L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(2)
                    .build();
            testCart.getItems().add(item);

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartCheckoutValidationResponse response = cartService.validateCartForCheckout(testUser.getId());

            // Assert
            assertNotNull(response);
            assertFalse(response.getIsValid());
            assertEquals(1, response.getIssues().size());
            CartItemIssueResponse issue = response.getIssues().get(0);
            assertEquals(1002L, issue.getCartItemId());
            assertEquals(activeVariant.getId(), issue.getProductVariantId());
            assertEquals("This item is no longer available.", issue.getMessage());
        }

        @Test
        @DisplayName("TC-CART-26 [Negative]: Product ACTIVE but Variant INACTIVE -> reports issue 'This item is no longer available.'")
        void validateCart_InactiveVariant_ReportsIssue() {
            // Arrange
            activeVariant.setStatus(ProductStatus.INACTIVE);
            CartItem item = CartItem.builder()
                    .id(1003L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(1)
                    .build();
            testCart.getItems().add(item);

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartCheckoutValidationResponse response = cartService.validateCartForCheckout(testUser.getId());

            // Assert
            assertNotNull(response);
            assertFalse(response.getIsValid());
            assertEquals(1, response.getIssues().size());
            assertEquals("This item is no longer available.", response.getIssues().get(0).getMessage());
        }

        @Test
        @DisplayName("TC-CART-27 [Negative]: Variant is out of stock (stock <= 0) -> reports issue 'This item is out of stock.'")
        void validateCart_OutOfStock_ReportsIssue() {
            // Arrange
            testInventory.setQuantity(0L);
            CartItem item = CartItem.builder()
                    .id(1004L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(1)
                    .build();
            testCart.getItems().add(item);

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartCheckoutValidationResponse response = cartService.validateCartForCheckout(testUser.getId());

            // Assert
            assertNotNull(response);
            assertFalse(response.getIsValid());
            assertEquals(1, response.getIssues().size());
            CartItemIssueResponse issue = response.getIssues().get(0);
            assertEquals(0L, issue.getAvailableStock());
            assertEquals("This item is out of stock.", issue.getMessage());
        }

        @Test
        @DisplayName("TC-CART-28 [Negative]: Requested quantity > available stock -> reports issue 'Only X items left in stock.'")
        void validateCart_InsufficientStock_ReportsIssue() {
            // Arrange (Stock is 5, requested is 8)
            testInventory.setQuantity(5L);
            CartItem item = CartItem.builder()
                    .id(1005L)
                    .cart(testCart)
                    .productVariant(activeVariant)
                    .quantity(8)
                    .build();
            testCart.getItems().add(item);

            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartCheckoutValidationResponse response = cartService.validateCartForCheckout(testUser.getId());

            // Assert
            assertNotNull(response);
            assertFalse(response.getIsValid());
            assertEquals(1, response.getIssues().size());
            CartItemIssueResponse issue = response.getIssues().get(0);
            assertEquals(5L, issue.getAvailableStock());
            assertEquals(8, issue.getRequestedQuantity());
            assertEquals("Only 5 items left in stock.", issue.getMessage());
        }

        @Test
        @DisplayName("TC-CART-29 [Boundary / Complex Combination]: Cart with 3 items having mixed issues -> all issues reported")
        void validateCart_MultipleIssues_ReportsAll() {
            // Arrange:
            // Item 1: Inactive product
            Product inactiveProd = Product.builder().id(201L).name("Old Laptop").status(ProductStatus.INACTIVE).build();
            ProductVariant variant1 = ProductVariant.builder().id(301L).product(inactiveProd).status(ProductStatus.ACTIVE).inventory(Inventory.builder().quantity(10L).build()).build();
            CartItem item1 = CartItem.builder().id(11L).cart(testCart).productVariant(variant1).quantity(2).build();

            // Item 2: Out of stock (stock = 0)
            Product activeProd2 = Product.builder().id(202L).name("Wireless Mouse").status(ProductStatus.ACTIVE).build();
            ProductVariant variant2 = ProductVariant.builder().id(302L).product(activeProd2).status(ProductStatus.ACTIVE).inventory(Inventory.builder().quantity(0L).build()).build();
            CartItem item2 = CartItem.builder().id(12L).cart(testCart).productVariant(variant2).quantity(1).build();

            // Item 3: Insufficient stock (stock = 2, requested = 5)
            Product activeProd3 = Product.builder().id(203L).name("Keyboard").status(ProductStatus.ACTIVE).build();
            ProductVariant variant3 = ProductVariant.builder().id(303L).product(activeProd3).status(ProductStatus.ACTIVE).inventory(Inventory.builder().quantity(2L).build()).build();
            CartItem item3 = CartItem.builder().id(13L).cart(testCart).productVariant(variant3).quantity(5).build();

            testCart.getItems().addAll(List.of(item1, item2, item3));
            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartCheckoutValidationResponse response = cartService.validateCartForCheckout(testUser.getId());

            // Assert
            assertNotNull(response);
            assertFalse(response.getIsValid());
            assertEquals(3, response.getIssues().size());
            assertEquals("This item is no longer available.", response.getIssues().get(0).getMessage());
            assertEquals("This item is out of stock.", response.getIssues().get(1).getMessage());
            assertEquals("Only 2 items left in stock.", response.getIssues().get(2).getMessage());
            assertEquals("Some items in your cart are no longer available in the requested quantity. Please update your cart to proceed.", response.getMessage());
        }
    }

    // =========================================================================
    // AC-CART-07: Calculation and Null-Safety Edge Cases (mapToCartResponse)
    // =========================================================================
    @Nested
    @DisplayName("AC-CART-07: Mapping & Null-Safety Edge Cases Tests")
    class StockAndMappingCalculationsTests {

        @Test
        @DisplayName("TC-CART-30 [Edge Case]: Variant has null price, null product, and null item quantity -> handled gracefully without NullPointerException")
        void mapToCartResponse_NullFields_HandledGracefully() {
            // Arrange
            ProductVariant variantWithNulls = ProductVariant.builder()
                    .id(999L)
                    .sku("NULL-SKU")
                    .price(null) // null price
                    .product(null) // null product
                    .inventory(null) // null inventory
                    .build();

            CartItem item = CartItem.builder()
                    .id(777L)
                    .cart(testCart)
                    .productVariant(variantWithNulls)
                    .quantity(null) // null quantity
                    .build();

            testCart.getItems().add(item);
            when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

            // Act
            CartResponse response = cartService.getCart(testUser.getId());

            // Assert
            assertNotNull(response);
            assertEquals(0, response.getTotalItems());
            assertEquals(BigDecimal.ZERO, response.getTotalPrice());
            assertEquals(1, response.getItems().size());

            var itemResp = response.getItems().get(0);
            assertEquals(BigDecimal.ZERO, itemResp.getUnitPrice());
            assertEquals(0, itemResp.getQuantity());
            assertEquals(BigDecimal.ZERO, itemResp.getSubtotal());
            assertEquals(0L, itemResp.getAvailableStock());
            assertTrue(itemResp.getIsOutOfStock());
            assertTrue(itemResp.getHasSufficientStock()); // 0 >= 0
            assertNull(itemResp.getProductId());
            assertEquals("", itemResp.getProductName());
        }
    }
}
