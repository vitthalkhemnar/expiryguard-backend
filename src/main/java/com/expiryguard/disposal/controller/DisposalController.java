package com.expiryguard.disposal.controller;

import com.expiryguard.disposal.dto.CreateDisposalRequest;
import com.expiryguard.disposal.dto.DisposalLogDto;
import com.expiryguard.disposal.service.DisposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/disposals")
@RequiredArgsConstructor
@Tag(name = "Disposal & Write-Off", description = "Endpoints for authorising and auditing disposal of expired or damaged inventory")
public class DisposalController {

    private final DisposalService disposalService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record and write off expired or damaged batch stock (Admin only)")
    public ResponseEntity<DisposalLogDto> recordDisposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateDisposalRequest request
    ) {
        return new ResponseEntity<>(
                disposalService.recordDisposal(userDetails.getUsername(), request),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit log of all stock disposals (Admin only)")
    public ResponseEntity<Page<DisposalLogDto>> getDisposalLogs(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(disposalService.getDisposalLogs(pageable));
    }
}
