package com.monsterhouse.booking.service;

import com.monsterhouse.booking.dto.request.ProductSaveRequest;
import com.monsterhouse.booking.dto.response.AdminProductResponse;
import com.monsterhouse.booking.entity.Product;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.booking.repository.ProductRepository;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;

    public List<AdminProductResponse> findAll() {
        return productRepository.findAll(
                        org.springframework.data.domain.Sort.by("sortOrder").ascending()
                                .and(org.springframework.data.domain.Sort.by("id").ascending()))
                .stream()
                .map(AdminProductResponse::of)
                .toList();
    }

    public AdminProductResponse findOne(Long productId) {
        return AdminProductResponse.of(getOrThrow(productId));
    }

    @Transactional
    public AdminProductResponse create(ProductSaveRequest request) {
        Product product = Product.builder()
                .type(request.type())
                .nameKo(request.nameKo())
                .nameJa(request.nameJa())
                .descriptionKo(request.descriptionKo())
                .descriptionJa(request.descriptionJa())
                .durationMin(request.durationMin())
                .price(request.price())
                .currency("KRW")
                .active(true)
                .sortOrder(request.sortOrder())
                .priceUnit(request.priceUnit())
                .bookable(request.bookable())
                .noteKo(request.noteKo())
                .noteJa(request.noteJa())
                .build();

        product.replaceIncludes(LocaleCode.KO, request.includesKo());
        product.replaceIncludes(LocaleCode.JA, request.includesJa());

        productRepository.save(product);
        log.info("Product created. id={} name={}", product.getId(), product.getNameKo());

        return AdminProductResponse.of(product);
    }

    @Transactional
    public AdminProductResponse update(Long productId, ProductSaveRequest request) {
        Product product = getOrThrow(productId);

        product.update(
                request.nameKo(), request.nameJa(),
                request.descriptionKo(), request.descriptionJa(),
                request.durationMin(), request.price(), request.sortOrder(),
                request.priceUnit(), request.bookable(), request.noteKo(), request.noteJa()
        );
        product.replaceIncludes(LocaleCode.KO, request.includesKo());
        product.replaceIncludes(LocaleCode.JA, request.includesJa());

        return AdminProductResponse.of(product);
    }

    @Transactional
    public AdminProductResponse setActive(Long productId, boolean active) {
        Product product = getOrThrow(productId);
        if (active) {
            product.activate();
        } else {
            product.deactivate();
        }
        return AdminProductResponse.of(product);
    }

    /**
     * 물리 삭제는 앞으로 예약이 하나도 없을 때만 허용합니다.
     *
     * 지난 예약은 booking.product_id 로 이 행을 참조하고 있어서 지우면 FK 가 깨지고,
     * 설령 지워지더라도 "무슨 상품을 촬영했는지" 이력이 사라집니다.
     * 운영에서 상품을 내리는 정상 경로는 삭제가 아니라 비활성화(setActive(false))입니다.
     */
    @Transactional
    public void delete(Long productId) {
        Product product = getOrThrow(productId);

        boolean hasBooking = bookingRepository.existsByProductIdAndStatusIn(
                productId, BookingStatus.occupyingStatuses());

        if (hasBooking) {
            log.info("Product delete rejected - bookings exist. id={}", productId);
            throw new BusinessException(ErrorCode.PRODUCT_IN_USE);
        }

        productRepository.delete(product);
        log.info("Product deleted. id={}", productId);
    }

    private Product getOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /** 미사용 경고 방지용 — 향후 통계에서 사용 예정 */
    LocalDateTime now() {
        return LocalDateTime.now();
    }
}