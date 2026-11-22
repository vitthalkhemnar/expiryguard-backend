package com.expiryguard.disposal.service;

import com.expiryguard.auth.model.User;
import com.expiryguard.auth.repository.UserRepository;
import com.expiryguard.batch.model.Batch;
import com.expiryguard.batch.model.BatchStatus;
import com.expiryguard.batch.repository.BatchRepository;
import com.expiryguard.common.exception.BusinessException;
import com.expiryguard.common.exception.ResourceNotFoundException;
import com.expiryguard.disposal.dto.CreateDisposalRequest;
import com.expiryguard.disposal.dto.DisposalLogDto;
import com.expiryguard.disposal.model.DisposalLog;
import com.expiryguard.disposal.repository.DisposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DisposalService {

    private final DisposalRepository disposalRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    @Transactional
    public DisposalLogDto recordDisposal(String managerUsername, CreateDisposalRequest request) {
        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + managerUsername));

        Batch batch = batchRepository.findById(request.batchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + request.batchId()));

        if (batch.getAvailableQuantity() < request.quantity()) {
            throw new BusinessException(
                    "Cannot dispose " + request.quantity() + " units. Only " + batch.getAvailableQuantity() + " units available in this batch."
            );
        }

        BigDecimal financialLoss = batch.getCostPrice().multiply(BigDecimal.valueOf(request.quantity()));

        batch.setAvailableQuantity(batch.getAvailableQuantity() - request.quantity());
        if (batch.getAvailableQuantity() == 0) {
            batch.setStatus(BatchStatus.DISPOSED);
        }
        batchRepository.save(batch);

        DisposalLog log = DisposalLog.builder()
                .batch(batch)
                .approvedBy(manager)
                .quantityDisposed(request.quantity())
                .financialLossAmount(financialLoss)
                .reason(request.reason())
                .notes(request.notes())
                .build();

        return mapToDto(disposalRepository.save(log));
    }

    @Transactional(readOnly = true)
    public Page<DisposalLogDto> getDisposalLogs(Pageable pageable) {
        return disposalRepository.findAllByOrderByDisposedAtDesc(pageable)
                .map(this::mapToDto);
    }

    private DisposalLogDto mapToDto(DisposalLog log) {
        return new DisposalLogDto(
                log.getId(),
                log.getBatch().getId(),
                log.getBatch().getBatchNumber(),
                log.getBatch().getProduct().getName(),
                log.getApprovedBy().getFullName(),
                log.getQuantityDisposed(),
                log.getFinancialLossAmount(),
                log.getReason(),
                log.getNotes(),
                log.getDisposedAt()
        );
    }
}
