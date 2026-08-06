package com.monsterhouse.common.init;

import com.monsterhouse.booking.entity.Availability;
import com.monsterhouse.booking.entity.PriceUnit;
import com.monsterhouse.booking.entity.Product;
import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.booking.repository.AvailabilityRepository;
import com.monsterhouse.booking.repository.ProductRepository;
import com.monsterhouse.common.enums.LocaleCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * 상품과 영업시간이 하나도 없으면 SlotService 가 모든 날짜를 CLOSED_DAY 로 돌려주고
 * 예약 화면이 영원히 "휴무"로 보입니다. 개발 중에 그걸 매번 손으로 넣지 않기 위한 시드입니다.
 *
 * ★ 가격은 실제 운영 가격표입니다.
 *
 * ⚠ 소요시간(durationMin)은 추정값입니다.
 *   실제 촬영 시간을 알려주시면 관리자 화면에서 바로 수정하시거나 여기 값을 고치면 됩니다.
 *   슬롯 길이를 결정하는 값이라 비워둘 수 없어서 넣어둔 것입니다.
 *
 * @Profile("local") 유지하세요. 운영 데이터는 관리자 화면에서 직접 입력합니다.
 */
@Slf4j
@Component
@Profile("local")
@Order(100)
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final AvailabilityRepository availabilityRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAvailability();
        seedProducts();
    }

    private void seedAvailability() {
        if (availabilityRepository.count() > 0) {
            return;
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean closed = day == DayOfWeek.MONDAY;   // 월요일 정기 휴무
            availabilityRepository.save(Availability.builder()
                    .dayOfWeek(day)
                    .openTime(LocalTime.of(10, 0))
                    .closeTime(LocalTime.of(20, 0))
                    .active(!closed)
                    .build());
        }
        log.info("[local] 영업시간 시드 생성 (월 휴무, 10:00~20:00)");
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        // ===================== 사진 =====================
        Product photo3 = photo(
                "보정본 3장", "レタッチ3枚",
                "촬영 후 보정본 3장을 전달드립니다.",
                "撮影後、レタッチ済み3枚をお渡しします。",
                250_000, 90, 1);
        addPhotoOptions(photo3);
        productRepository.save(photo3);

        Product photo4 = photo(
                "보정본 4장", "レタッチ4枚",
                "촬영 후 보정본 4장을 전달드립니다.",
                "撮影後、レタッチ済み4枚をお渡しします。",
                300_000, 90, 2);
        addPhotoOptions(photo4);
        productRepository.save(photo4);

        // ===================== 영상 =====================
        // "* 사진촬영 별도" — 영상 상품에는 사진이 포함되지 않습니다.
        String videoNoteKo = "사진 촬영은 별도입니다.";
        String videoNoteJa = "写真撮影は別料金です。";

        productRepository.save(video(
                "포징영상", "ポージング映像",
                "무대 포징을 영상으로 남깁니다.",
                "ステージのポージングを映像で残します。",
                100_000, 30, 3, videoNoteKo, videoNoteJa));

        productRepository.save(video(
                "모티베이션", "モチベーション",
                "훈련하는 모습을 그대로 담는 영상 촬영.",
                "トレーニングの姿をそのまま収める映像撮影。",
                250_000, 60, 4, videoNoteKo, videoNoteJa));

        productRepository.save(video(
                "포징 + 모티베이션", "ポージング + モチベーション",
                "포징영상과 모티베이션 영상을 함께 촬영합니다.",
                "ポージング映像とモチベーション映像をまとめて撮影します。",
                300_000, 90, 5, videoNoteKo, videoNoteJa));

        // ===================== 통역 =====================
        // 대회 일정에 맞춰야 해서 온라인 슬롯 예약 대상이 아닙니다(bookable = false).
        // 가격표만 노출하고 신청은 통역 문의 폼으로 받습니다.
        Product support = Product.builder()
                .type(ProductType.INTERPRETER)
                .nameKo("대회 서포트").nameJa("大会サポート")
                .descriptionKo("대회 당일 백스테이지 동행, 포징 체크, 현장 스냅사진 촬영이 포함됩니다.")
                .descriptionJa("大会当日のバックステージ同行、ポージングチェック、現場スナップ撮影が含まれます。")
                .durationMin(0)
                .price(BigDecimal.valueOf(100_000))
                .currency("KRW")
                .priceUnit(PriceUnit.PER_DAY)
                .bookable(false)
                .active(true)
                .sortOrder(6)
                .build();
        support.replaceIncludes(LocaleCode.KO,
                List.of("백스테이지 동행", "포징 체크", "현장 스냅사진 촬영"));
        support.replaceIncludes(LocaleCode.JA,
                List.of("バックステージ同行", "ポージングチェック", "現場スナップ撮影"));
        productRepository.save(support);

        productRepository.save(Product.builder()
                .type(ProductType.INTERPRETER)
                .nameKo("PT 통역").nameJa("PT通訳")
                .descriptionKo("퍼스널 트레이닝 세션 통역.")
                .descriptionJa("パーソナルトレーニングセッションの通訳。")
                .durationMin(0)
                .price(BigDecimal.valueOf(50_000))
                .currency("KRW")
                .priceUnit(PriceUnit.PER_HOUR)
                .bookable(false)
                .active(true)
                .sortOrder(7)
                .build());

        log.info("[local] 촬영 상품 시드 7건 생성 (사진2 · 영상3 · 통역2)");
    }

    // ===================== 헬퍼 =====================

    private Product photo(String nameKo, String nameJa, String descKo, String descJa,
                          int price, int durationMin, int sortOrder) {
        return Product.builder()
                .type(ProductType.PHOTO)
                .nameKo(nameKo).nameJa(nameJa)
                .descriptionKo(descKo).descriptionJa(descJa)
                .durationMin(durationMin)
                .price(BigDecimal.valueOf(price))
                .currency("KRW")
                .priceUnit(PriceUnit.PER_SESSION)
                .bookable(true)
                .active(true)
                .sortOrder(sortOrder)
                .build();
    }

    private Product video(String nameKo, String nameJa, String descKo, String descJa,
                          int price, int durationMin, int sortOrder,
                          String noteKo, String noteJa) {
        return Product.builder()
                .type(ProductType.VIDEO)
                .nameKo(nameKo).nameJa(nameJa)
                .descriptionKo(descKo).descriptionJa(descJa)
                .durationMin(durationMin)
                .price(BigDecimal.valueOf(price))
                .currency("KRW")
                .priceUnit(PriceUnit.PER_SESSION)
                .bookable(true)
                .active(true)
                .sortOrder(sortOrder)
                .noteKo(noteKo).noteJa(noteJa)
                .build();
    }

    /** 사진 상품 공통 옵션 — 보정본 추가(장당), 포징영상 추가 */
    private void addPhotoOptions(Product product) {
        product.addOption("보정본 1장 추가", "レタッチ1枚追加",
                BigDecimal.valueOf(50_000), 10, 1);
        product.addOption("포징영상 추가", "ポージング映像追加",
                BigDecimal.valueOf(50_000), 1, 2);
    }
}
