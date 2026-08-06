package com.monsterhouse.booking.exception;

import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;

/**
 * 동시성 방어 2층(overlap 쿼리) 또는 3층(UNIQUE 위반)에서 발생.
 * 어느 층에서 걸렸든 사용자에게는 같은 메시지를 보여줍니다.
 */
public class SlotAlreadyTakenException extends BusinessException {

    public SlotAlreadyTakenException() {
        super(ErrorCode.SLOT_ALREADY_TAKEN);
    }
}