package com.acc.gazua.domain.roominventory.repository;

import com.acc.gazua.domain.roominventory.entity.RoomInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoomInventoryJdbcRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void bulkInsert(List<RoomInventory> inventories){
        String sql = "INSERT INTO room_inventory(room_id,date,price,total_count,available_count) "
                + "VALUES(:roomId,:date,:price,:totalCount,:availableCount) "
                + "ON DUPLICATE KEY UPDATE "
                + "total_count = :totalCount, available_count = :availableCount";

        SqlParameterSource[] params = inventories.stream()
                .map(inventory -> new MapSqlParameterSource()
                        .addValue("roomId", inventory.getRoom().getId())
                        .addValue("date", inventory.getDate())
                        .addValue("price", inventory.getPrice())
                        .addValue("totalCount", inventory.getTotalCount())
                        .addValue("availableCount", inventory.getAvailableCount()))
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql,params);
    }
}
