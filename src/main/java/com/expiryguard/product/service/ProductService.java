package com.expiryguard.product.service;

import com.expiryguard.batch.repository.BatchRepository;
import com.expiryguard.common.exception.BusinessException;
import com.expiryguard.common.exception.ResourceNotFoundException;
import com.expiryguard.product.dto.CategoryDto;
import com.expiryguard.product.dto.CreateProductRequest;
import com.expiryguard.product.dto.ProductDto;
import com.expiryguard.product.model.Category;
import com.expiryguard.product.model.Product;
import com.expiryguard.product.repository.CategoryRepository;
import com.expiryguard.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BatchRepository batchRepository;

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(String search, Long categoryId, Pageable pageable) {
        String searchPattern = (search != null && !search.trim().isEmpty())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;
        return productRepository.searchProducts(searchPattern, categoryId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToDto(product);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
        return mapToDto(product);
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException("A product with SKU '" + request.sku() + "' already exists");
        }
        if (productRepository.existsByBarcode(request.barcode())) {
            throw new BusinessException("A product with barcode '" + request.barcode() + "' already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        Product product = Product.builder()
                .sku(request.sku())
                .barcode(request.barcode())
                .name(request.name())
                .description(request.description())
                .category(category)
                .minReorderLevel(request.minReorderLevel())
                .active(true)
                .build();

        return mapToDto(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }

    private ProductDto mapToDto(Product product) {
        int availableStock = batchRepository.sumAvailableQuantityByProductId(product.getId(), LocalDate.now());
        CategoryDto categoryDto = new CategoryDto(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription()
        );

        return new ProductDto(
                product.getId(),
                product.getSku(),
                product.getBarcode(),
                product.getName(),
                product.getDescription(),
                categoryDto,
                product.getMinReorderLevel(),
                product.isActive(),
                product.getCreatedAt(),
                availableStock
        );
    }
}
