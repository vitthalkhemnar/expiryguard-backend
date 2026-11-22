package com.expiryguard.batch.service;

import com.expiryguard.batch.dto.BatchDto;
import com.expiryguard.batch.dto.CreateBatchRequest;
import com.expiryguard.batch.model.Batch;
import com.expiryguard.batch.model.BatchStatus;
import com.expiryguard.batch.repository.BatchRepository;
import com.expiryguard.common.exception.BusinessException;
import com.expiryguard.common.exception.ResourceNotFoundException;
import com.expiryguard.product.model.Product;
import com.expiryguard.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<BatchDto> getAllBatches() {
        return batchRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BatchDto> getBatchesByProduct(Long productId) {
        return batchRepository.findByProductId(productId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchDto getBatchById(Long id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
        return mapToDto(batch);
    }

    @Transactional
    public BatchDto createBatch(CreateBatchRequest request) {
        if (batchRepository.existsByBatchNumber(request.batchNumber())) {
            throw new BusinessException("A batch with number '" + request.batchNumber() + "' already exists");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (request.expiryDate().isBefore(LocalDate.now()) || request.expiryDate().isEqual(LocalDate.now())) {
            throw new BusinessException("Cannot register a batch that is already expired or expiring today");
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), request.expiryDate());
        BatchStatus initialStatus = BatchStatus.ACTIVE;
        if (daysUntilExpiry <= 7) {
            initialStatus = BatchStatus.CRITICAL;
        } else if (daysUntilExpiry <= 30) {
            initialStatus = BatchStatus.NEAR_EXPIRY;
        }

        Batch batch = Batch.builder()
                .product(product)
                .batchNumber(request.batchNumber())
                .initialQuantity(request.initialQuantity())
                .availableQuantity(request.initialQuantity())
                .costPrice(request.costPrice())
                .baseSellingPrice(request.baseSellingPrice())
                .currentSellingPrice(request.baseSellingPrice())
                .discountPercentage(0)
                .manufacturingDate(request.manufacturingDate())
                .expiryDate(request.expiryDate())
                .status(initialStatus)
                .build();

        return mapToDto(batchRepository.save(batch));
    }

    public BatchDto mapToDto(Batch batch) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
        return new BatchDto(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getProduct().getName(),
                batch.getProduct().getSku(),
                batch.getBatchNumber(),
                batch.getInitialQuantity(),
                batch.getAvailableQuantity(),
                batch.getCostPrice(),
                batch.getBaseSellingPrice(),
                batch.getCurrentSellingPrice(),
                batch.getDiscountPercentage(),
                batch.getManufacturingDate(),
                batch.getExpiryDate(),
                daysUntil,
                batch.getStatus(),
                batch.getCreatedAt()
        );
    }
}
