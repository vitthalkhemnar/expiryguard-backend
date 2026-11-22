package com.expiryguard.pos.service;

import com.expiryguard.auth.model.User;
import com.expiryguard.auth.repository.UserRepository;
import com.expiryguard.batch.model.Batch;
import com.expiryguard.batch.repository.BatchRepository;
import com.expiryguard.common.exception.ExpiredStockException;
import com.expiryguard.common.exception.InsufficientStockException;
import com.expiryguard.common.exception.ResourceNotFoundException;
import com.expiryguard.pos.dto.*;
import com.expiryguard.pos.model.Order;
import com.expiryguard.pos.model.OrderItem;
import com.expiryguard.pos.model.PaymentStatus;
import com.expiryguard.pos.repository.OrderRepository;
import com.expiryguard.product.model.Product;
import com.expiryguard.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PosService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartPreviewResponse previewCart(PreviewCartRequest request) {
        LocalDate today = LocalDate.now();
        List<CartPreviewItemDto> previewItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (CartItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.productId()));

            // Query all available unexpired batches with clearance discounts for this product
            List<Batch> discountedBatches = batchRepository.findDiscountedBatchesForProduct(product.getId(), today);
            List<ClearanceOptionDto> availableDiscounts = discountedBatches.stream()
                    .map(b -> new ClearanceOptionDto(
                            b.getId(),
                            b.getBatchNumber(),
                            b.getDiscountPercentage(),
                            b.getCurrentSellingPrice(),
                            b.getBaseSellingPrice(),
                            b.getExpiryDate(),
                            ChronoUnit.DAYS.between(today, b.getExpiryDate()),
                            b.getAvailableQuantity()
                    ))
                    .toList();

            List<Batch> batches = resolveBatchesForItem(product, itemReq, today);
            int remainingNeeded = itemReq.quantity();
            int totalAvailable = batches.stream().mapToInt(Batch::getAvailableQuantity).sum();

            if (totalAvailable < remainingNeeded) {
                String stockType = itemReq.selectedBatchId() != null
                        ? "selected clearance batch"
                        : (itemReq.isPreferExpiringDiscount() ? "available" : "fresh unexpired");
                throw new InsufficientStockException(
                        "Insufficient " + stockType + " stock for '" + product.getName() + "'. Requested: " + remainingNeeded + ", Available: " + totalAvailable
                );
            }

            List<AllocatedBatchItemDto> allocatedBatches = new ArrayList<>();
            BigDecimal itemSubtotal = BigDecimal.ZERO;
            BigDecimal itemDiscount = BigDecimal.ZERO;

            for (Batch batch : batches) {
                if (remainingNeeded <= 0) break;

                int allocateFromThis = Math.min(remainingNeeded, batch.getAvailableQuantity());
                long daysUntil = ChronoUnit.DAYS.between(today, batch.getExpiryDate());

                BigDecimal lineBase = batch.getBaseSellingPrice().multiply(BigDecimal.valueOf(allocateFromThis));
                BigDecimal lineCurrent = batch.getCurrentSellingPrice().multiply(BigDecimal.valueOf(allocateFromThis));
                BigDecimal lineDisc = lineBase.subtract(lineCurrent);

                allocatedBatches.add(new AllocatedBatchItemDto(
                        batch.getId(),
                        batch.getBatchNumber(),
                        batch.getExpiryDate(),
                        daysUntil,
                        allocateFromThis,
                        batch.getBaseSellingPrice(),
                        batch.getCurrentSellingPrice(),
                        batch.getDiscountPercentage(),
                        lineDisc,
                        lineCurrent
                ));

                itemSubtotal = itemSubtotal.add(lineBase);
                itemDiscount = itemDiscount.add(lineDisc);
                remainingNeeded -= allocateFromThis;
            }

            BigDecimal itemTotal = itemSubtotal.subtract(itemDiscount);
            previewItems.add(new CartPreviewItemDto(
                    product.getId(),
                    product.getName(),
                    product.getBarcode(),
                    product.getSku(),
                    itemReq.quantity(),
                    itemReq.selectedBatchId(),
                    itemReq.isPreferExpiringDiscount(),
                    availableDiscounts,
                    allocatedBatches,
                    itemSubtotal,
                    itemDiscount,
                    itemTotal
            ));

            subtotal = subtotal.add(itemSubtotal);
            totalDiscount = totalDiscount.add(itemDiscount);
        }

        BigDecimal totalPayable = subtotal.subtract(totalDiscount);
        return new CartPreviewResponse(previewItems, subtotal, totalDiscount, totalPayable);
    }

    @Transactional
    public OrderResponseDto checkout(String cashierUsername, CheckoutRequest request) {
        LocalDate today = LocalDate.now();
        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Cashier not found: " + cashierUsername));

        String orderNumber = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        String custName = (request.customerName() != null && !request.customerName().isBlank())
                ? request.customerName().trim()
                : "Walk-in Customer";
        String custPhone = (request.customerPhone() != null && !request.customerPhone().isBlank())
                ? request.customerPhone().trim()
                : null;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .cashier(cashier)
                .customerName(custName)
                .customerPhone(custPhone)
                .paymentMethod(request.paymentMethod())
                .paymentStatus(PaymentStatus.PAID)
                .subtotal(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (CartItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.productId()));

            List<Batch> batches = resolveBatchesForItem(product, itemReq, today);
            int remainingNeeded = itemReq.quantity();
            int totalAvailable = batches.stream().mapToInt(Batch::getAvailableQuantity).sum();

            if (totalAvailable < remainingNeeded) {
                String stockType = itemReq.selectedBatchId() != null
                        ? "selected clearance batch"
                        : (itemReq.isPreferExpiringDiscount() ? "available" : "fresh unexpired");
                throw new InsufficientStockException(
                        "Insufficient " + stockType + " stock for '" + product.getName() + "'. Requested: " + remainingNeeded + ", Available: " + totalAvailable
                );
            }

            for (Batch batch : batches) {
                if (remainingNeeded <= 0) break;

                // Zero-Expired Sales Guardrail
                if (batch.isExpired()) {
                    throw new ExpiredStockException(
                            "Sale blocked: Batch '" + batch.getBatchNumber() + "' for product '" + product.getName() + "' has expired on " + batch.getExpiryDate()
                    );
                }

                int allocateFromThis = Math.min(remainingNeeded, batch.getAvailableQuantity());
                batch.setAvailableQuantity(batch.getAvailableQuantity() - allocateFromThis);

                BigDecimal unitBase = batch.getBaseSellingPrice();
                BigDecimal unitCurrent = batch.getCurrentSellingPrice();
                BigDecimal unitDisc = unitBase.subtract(unitCurrent);

                BigDecimal lineBase = unitBase.multiply(BigDecimal.valueOf(allocateFromThis));
                BigDecimal lineCurrent = unitCurrent.multiply(BigDecimal.valueOf(allocateFromThis));
                BigDecimal lineDisc = lineBase.subtract(lineCurrent);

                OrderItem orderItem = OrderItem.builder()
                        .batch(batch)
                        .quantity(allocateFromThis)
                        .unitCostPrice(batch.getCostPrice())
                        .unitSellingPrice(unitCurrent)
                        .discountApplied(lineDisc)
                        .totalPrice(lineCurrent)
                        .build();

                order.addItem(orderItem);

                subtotal = subtotal.add(lineBase);
                totalDiscount = totalDiscount.add(lineDisc);
                remainingNeeded -= allocateFromThis;
            }

            batchRepository.saveAll(batches);
        }

        order.setSubtotal(subtotal);
        order.setTotalDiscount(totalDiscount);
        order.setTotalAmount(subtotal.subtract(totalDiscount));

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrders(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            return orderRepository.searchOrders(searchPattern, pageable)
                    .map(this::mapToOrderResponse);
        }
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrders(Pageable pageable) {
        return getOrders(null, pageable);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return mapToOrderResponse(order);
    }

    private OrderResponseDto mapToOrderResponse(Order order) {
        List<OrderResponseDto.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderResponseDto.OrderItemDto(
                        item.getId(),
                        item.getBatch().getProduct().getName(),
                        item.getBatch().getProduct().getSku(),
                        item.getBatch().getBatchNumber(),
                        item.getQuantity(),
                        item.getUnitSellingPrice(),
                        item.getDiscountApplied(),
                        item.getTotalPrice()
                ))
                .toList();

        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getCashier() != null ? order.getCashier().getFullName() : "N/A",
                order.getCustomerName() != null ? order.getCustomerName() : "Walk-in Customer",
                order.getCustomerPhone(),
                order.getSubtotal(),
                order.getTotalDiscount(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                itemDtos,
                order.getCreatedAt()
        );
    }

    private List<Batch> resolveBatchesForItem(Product product, CartItemRequest itemReq, LocalDate today) {
        if (itemReq.selectedBatchId() != null) {
            Batch selectedBatch = batchRepository.findById(itemReq.selectedBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected batch not found with id: " + itemReq.selectedBatchId()));

            if (!selectedBatch.getProduct().getId().equals(product.getId())) {
                throw new com.expiryguard.common.exception.BusinessException("Selected batch does not belong to product: " + product.getName());
            }

            List<Batch> batches = new ArrayList<>();
            batches.add(selectedBatch);

            // If requested quantity exceeds the selected batch's available stock, append other available FEFO batches
            if (itemReq.quantity() > selectedBatch.getAvailableQuantity()) {
                List<Batch> otherBatches = batchRepository.findAvailableBatchesForFefo(product.getId(), today);
                for (Batch b : otherBatches) {
                    if (!b.getId().equals(selectedBatch.getId())) {
                        batches.add(b);
                    }
                }
            }
            return batches;
        }

        return itemReq.isPreferExpiringDiscount()
                ? batchRepository.findAvailableBatchesForFefo(product.getId(), today)
                : batchRepository.findFreshBatchesForProduct(product.getId(), today);
    }
}
