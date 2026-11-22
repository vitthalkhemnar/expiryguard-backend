package com.expiryguard;

import com.expiryguard.auth.model.Role;
import com.expiryguard.auth.model.User;
import com.expiryguard.auth.repository.UserRepository;
import com.expiryguard.batch.model.Batch;
import com.expiryguard.batch.model.BatchStatus;
import com.expiryguard.batch.repository.BatchRepository;
import com.expiryguard.common.exception.ExpiredStockException;
import com.expiryguard.common.exception.InsufficientStockException;
import com.expiryguard.pos.dto.CartItemRequest;
import com.expiryguard.pos.dto.CartPreviewResponse;
import com.expiryguard.pos.dto.CheckoutRequest;
import com.expiryguard.pos.dto.PreviewCartRequest;
import com.expiryguard.pos.model.PaymentMethod;
import com.expiryguard.pos.repository.OrderRepository;
import com.expiryguard.pos.service.PosService;
import com.expiryguard.product.model.Category;
import com.expiryguard.product.model.Product;
import com.expiryguard.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FefoAllocationTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PosService posService;

    private Product testProduct;
    private Batch earlierBatch;
    private Batch laterBatch;
    private User testCashier;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(1L).name("Pharma").build();
        testProduct = Product.builder()
                .id(100L)
                .name("Amoxicillin 500mg")
                .sku("AMOX-500")
                .barcode("123456789")
                .category(category)
                .build();

        // Batch 1 expires in 5 days, 3 units available, discounted to $50 (50% off)
        earlierBatch = Batch.builder()
                .id(1L)
                .product(testProduct)
                .batchNumber("BATCH-EARLY")
                .initialQuantity(10)
                .availableQuantity(3)
                .costPrice(new BigDecimal("30.00"))
                .baseSellingPrice(new BigDecimal("100.00"))
                .currentSellingPrice(new BigDecimal("50.00"))
                .discountPercentage(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .status(BatchStatus.CRITICAL)
                .build();

        // Batch 2 expires in 60 days, 10 units available, regular price $100 (0% off)
        laterBatch = Batch.builder()
                .id(2L)
                .product(testProduct)
                .batchNumber("BATCH-LATER")
                .initialQuantity(20)
                .availableQuantity(10)
                .costPrice(new BigDecimal("30.00"))
                .baseSellingPrice(new BigDecimal("100.00"))
                .currentSellingPrice(new BigDecimal("100.00"))
                .discountPercentage(0)
                .expiryDate(LocalDate.now().plusDays(60))
                .status(BatchStatus.ACTIVE)
                .build();

        testCashier = User.builder()
                .id(5L)
                .username("cashier")
                .fullName("Jane Cashier")
                .role(Role.ROLE_CASHIER)
                .build();
    }

    @Test
    @DisplayName("Opt-In: When customer opts into clearance discount, allocate from earliest batch first")
    void testFefoAllocationSplitAcrossBatches() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(batchRepository.findAvailableBatchesForFefo(eq(100L), any(LocalDate.class)))
                .thenReturn(List.of(earlierBatch, laterBatch));

        // preferExpiringDiscount = true (default)
        PreviewCartRequest request = new PreviewCartRequest(List.of(new CartItemRequest(100L, 5, true)));
        CartPreviewResponse preview = posService.previewCart(request);

        assertThat(preview.items()).hasSize(1);
        var previewItem = preview.items().get(0);
        assertThat(previewItem.requestedQuantity()).isEqualTo(5);
        assertThat(previewItem.preferExpiringDiscount()).isTrue();
        assertThat(previewItem.allocatedBatches()).hasSize(2);

        // 3 units from BATCH-EARLY at $50 = $150
        var alloc1 = previewItem.allocatedBatches().get(0);
        assertThat(alloc1.batchNumber()).isEqualTo("BATCH-EARLY");
        assertThat(alloc1.allocatedQuantity()).isEqualTo(3);
        assertThat(alloc1.lineTotal()).isEqualByComparingTo("150.00");

        // 2 units from BATCH-LATER at $100 = $200
        var alloc2 = previewItem.allocatedBatches().get(1);
        assertThat(alloc2.batchNumber()).isEqualTo("BATCH-LATER");
        assertThat(alloc2.allocatedQuantity()).isEqualTo(2);
        assertThat(alloc2.lineTotal()).isEqualByComparingTo("200.00");

        // Subtotal = 5 * 100 = 500. Discount = 3 * 50 = 150. Total payable = 350
        assertThat(preview.subtotal()).isEqualByComparingTo("500.00");
        assertThat(preview.totalDiscount()).isEqualByComparingTo("150.00");
        assertThat(preview.totalPayable()).isEqualByComparingTo("350.00");
    }

    @Test
    @DisplayName("Opt-Out: When customer opts out of clearance discount, allocate fresh unexpired stock at full price")
    void testOptOutOfExpiringDiscountGetsFreshBatch() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(batchRepository.findFreshBatchesForProduct(eq(100L), any(LocalDate.class)))
                .thenReturn(List.of(laterBatch, earlierBatch));

        // preferExpiringDiscount = false (Opt-out)
        PreviewCartRequest request = new PreviewCartRequest(List.of(new CartItemRequest(100L, 2, false)));
        CartPreviewResponse preview = posService.previewCart(request);

        assertThat(preview.items()).hasSize(1);
        var previewItem = preview.items().get(0);
        assertThat(previewItem.requestedQuantity()).isEqualTo(2);
        assertThat(previewItem.preferExpiringDiscount()).isFalse();
        assertThat(previewItem.allocatedBatches()).hasSize(1);

        // All 2 units allocated from fresh BATCH-LATER at full price $100
        var alloc = previewItem.allocatedBatches().get(0);
        assertThat(alloc.batchNumber()).isEqualTo("BATCH-LATER");
        assertThat(alloc.allocatedQuantity()).isEqualTo(2);
        assertThat(alloc.currentPrice()).isEqualByComparingTo("100.00");
        assertThat(alloc.discountPercentage()).isEqualTo(0);

        // Zero discount given
        assertThat(preview.totalDiscount()).isEqualByComparingTo("0.00");
        assertThat(preview.totalPayable()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Inventory: When requesting more units than available, throw InsufficientStockException")
    void testInsufficientStockException() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(batchRepository.findAvailableBatchesForFefo(eq(100L), any(LocalDate.class)))
                .thenReturn(List.of(earlierBatch, laterBatch));

        PreviewCartRequest request = new PreviewCartRequest(List.of(new CartItemRequest(100L, 15, true)));

        assertThatThrownBy(() -> posService.previewCart(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("Zero-Expired Guardrail: Expired batch in checkout throws ExpiredStockException")
    void testZeroExpiredSaleThrowsException() {
        earlierBatch.setExpiryDate(LocalDate.now().minusDays(1)); // Expired yesterday
        earlierBatch.setStatus(BatchStatus.EXPIRED);

        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(testCashier));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(batchRepository.findAvailableBatchesForFefo(eq(100L), any(LocalDate.class)))
                .thenReturn(List.of(earlierBatch));

        CheckoutRequest checkoutReq = new CheckoutRequest(
                List.of(new CartItemRequest(100L, 2, true)),
                PaymentMethod.CASH
        );

        assertThatThrownBy(() -> posService.checkout("cashier", checkoutReq))
                .isInstanceOf(ExpiredStockException.class)
                .hasMessageContaining("Sale blocked");
    }

    @Test
    @DisplayName("Clearance Selection: When customer selects specific clearance discount batch, allocate from chosen batch")
    void testSelectSpecificClearanceBatch() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(batchRepository.findById(1L)).thenReturn(Optional.of(earlierBatch));
        when(batchRepository.findDiscountedBatchesForProduct(eq(100L), any(LocalDate.class)))
                .thenReturn(List.of(earlierBatch));

        // Selected batch 1L explicitly (50% discount)
        CartItemRequest itemReq = new CartItemRequest(100L, 3, true, 1L);
        PreviewCartRequest request = new PreviewCartRequest(List.of(itemReq));
        CartPreviewResponse preview = posService.previewCart(request);

        assertThat(preview.items()).hasSize(1);
        var previewItem = preview.items().get(0);
        assertThat(previewItem.selectedBatchId()).isEqualTo(1L);
        assertThat(previewItem.availableDiscounts()).hasSize(1);
        assertThat(previewItem.availableDiscounts().get(0).discountPercentage()).isEqualTo(50);
        assertThat(previewItem.availableDiscounts().get(0).batchNumber()).isEqualTo("BATCH-EARLY");

        // 3 units from earlierBatch @ $50 = $150
        assertThat(previewItem.allocatedBatches()).hasSize(1);
        assertThat(previewItem.allocatedBatches().get(0).batchNumber()).isEqualTo("BATCH-EARLY");
        assertThat(previewItem.allocatedBatches().get(0).discountPercentage()).isEqualTo(50);
        assertThat(preview.totalDiscount()).isEqualByComparingTo("150.00");
        assertThat(preview.totalPayable()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Single Table Storage: Checkout records customer name and mobile directly into Order entity")
    void testCheckoutRecordsCustomerDetailsInOrder() {
        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(testCashier));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(batchRepository.findAvailableBatchesForFefo(eq(100L), any(LocalDate.class)))
                .thenReturn(List.of(laterBatch));
        when(orderRepository.save(any(com.expiryguard.pos.model.Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(
                List.of(new CartItemRequest(100L, 2, true)),
                PaymentMethod.UPI,
                "Rajesh Mehta",
                "9820112233"
        );

        var response = posService.checkout("cashier", request);

        assertThat(response).isNotNull();
        assertThat(response.customerName()).isEqualTo("Rajesh Mehta");
        assertThat(response.customerPhone()).isEqualTo("9820112233");
        assertThat(response.cashierName()).isEqualTo("Jane Cashier");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.UPI);

        verify(orderRepository).save(argThat(order ->
                "Rajesh Mehta".equals(order.getCustomerName()) &&
                "9820112233".equals(order.getCustomerPhone())
        ));
    }
}
