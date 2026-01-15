package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.outbox.dto.DeadOutBoxEventsRetryRequest;
import com.codeit.mopl.outbox.dto.OutBoxSearchRequest;
import com.codeit.mopl.outbox.entity.OutBoxEvent;

import java.util.List;

public interface CustomOutBoxEventRepository {
    List<OutBoxEvent> findByCursor(OutBoxSearchRequest request);

    List<OutBoxEvent> findDeadOutBoxEventsByConditions(DeadOutBoxEventsRetryRequest request);
}
