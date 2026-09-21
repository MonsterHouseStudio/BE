package com.monsterhouse.booking.controller;

import com.monsterhouse.booking.dto.response.ProductResponse;
import com.monsterhouse.booking.entity.Product;
import com.monsterhouse.booking.repository.ProductRepository;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductController {
    private final ProductRepository productRepository;
    private final StorageService storageService;
    @GetMapping
    public ApiResponse<List<ProductResponse>>list(){
        LocaleCode locale = currentLocale();
        List<ProductResponse> products = productRepository
                .findAllByActiveTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(p -> ProductResponse.of(p, locale, storageService.url(p.getImageKey())))
                .toList();
        return ApiResponse.ok(products);
    }
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long productId){
        Product product = productRepository.findByIdAndActiveTrue(productId).orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ApiResponse.ok(ProductResponse.of(product, currentLocale(), storageService.url(product.getImageKey())));
    }
    private LocaleCode currentLocale(){
        return LocaleCode.from(LocaleContextHolder.getLocale());
    }
}
