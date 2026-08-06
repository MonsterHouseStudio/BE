package com.monsterhouse.booking.repository;

import com.monsterhouse.booking.dto.request.BookingSearchCondition;
import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.monsterhouse.booking.entity.QBooking.booking;
import static com.monsterhouse.booking.entity.QProduct.product;

/**
 * 구현체 이름은 반드시 "프래그먼트 인터페이스명 + Impl" 이어야 합니다.
 * BookingSearchRepositoryImpl 이 아니면 Spring Data 가 찾지 못하고
 * "No property 'search' found" 로 기동에 실패합니다.
 */
@RequiredArgsConstructor
public class BookingSearchRepositoryImpl implements BookingSearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Booking> search(BookingSearchCondition condition, Pageable pageable) {

        List<Booking> content = queryFactory
                .selectFrom(booking)
                // 목록에서 상품명을 쓰므로 미리 붙입니다. 없으면 행 수만큼 추가 쿼리가 나갑니다(N+1).
                .join(booking.product, product).fetchJoin()
                .where(
                        statusEq(condition.status()),
                        startAtGoe(condition.from()),
                        startAtLt(condition.to()),
                        productIdEq(condition.productId()),
                        keywordContains(condition.keyword())
                )
                .orderBy(booking.startAt.desc(), booking.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 는 fetchJoin 없이. 조인이 붙으면 불필요하게 무거워집니다.
        JPAQuery<Long> countQuery = queryFactory
                .select(booking.count())
                .from(booking)
                .where(
                        statusEq(condition.status()),
                        startAtGoe(condition.from()),
                        startAtLt(condition.to()),
                        productIdEq(condition.productId()),
                        keywordContains(condition.keyword())
                );

        // 마지막 페이지처럼 count 가 필요 없는 경우엔 아예 실행하지 않습니다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // null 을 반환하면 QueryDSL 이 해당 조건을 통째로 무시합니다.
    // 이게 동적 쿼리를 if 문 없이 쓰는 핵심입니다.

    private BooleanExpression statusEq(BookingStatus status) {
        return status == null ? null : booking.status.eq(status);
    }

    private BooleanExpression startAtGoe(LocalDate from) {
        return from == null ? null : booking.startAt.goe(from.atStartOfDay());
    }

    /** to 는 그 날짜를 포함해야 하므로 다음 날 00:00 미만으로 겁니다. */
    private BooleanExpression startAtLt(LocalDate to) {
        return to == null ? null : booking.startAt.lt(to.plusDays(1).atStartOfDay());
    }

    private BooleanExpression productIdEq(Long productId) {
        return productId == null ? null : booking.product.id.eq(productId);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String value = keyword.trim();
        return booking.name.containsIgnoreCase(value)
                .or(booking.phone.containsIgnoreCase(value))
                .or(booking.email.containsIgnoreCase(value))
                .or(booking.bookingCode.containsIgnoreCase(value));
    }
}