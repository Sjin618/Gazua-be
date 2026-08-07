package com.acc.gazua.domain.roominventory.service;

import com.acc.gazua.domain.roominventory.repository.RoomInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomInventoryService {
    private final RoomInventoryRepository roomInventoryRepository;
}
