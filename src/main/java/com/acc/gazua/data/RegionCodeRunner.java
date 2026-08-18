package com.acc.gazua.data;

import com.acc.gazua.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RegionCodeRunner implements CommandLineRunner {
    private final SearchRegionCode searchRegionCode;
    private final RegionRepository regionRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Override
    public void run(String... args) throws Exception {
        //2056번 실행
        for (int i = 1; i <= 5; i++) {
            List<RegionCodeResponse.Row> rows = searchRegionCode.searchRegionCode(i);
            createAndSaveRegionCode(rows);
        }
    }

    private void createAndSaveRegionCode(List<RegionCodeResponse.Row> rows){
        String sql = "INSERT INTO region(region_code,sido_code,sigungu_code,umd_code,ri_code,region_name) "
                + "VALUES(:regionCode,:sidoCode,:sigunguCode,:umdCode,:riCode,:regionName) ";

        SqlParameterSource[] params = rows.stream()
                .map(row -> new MapSqlParameterSource()
                        .addValue("regionCode", row.getRegionCd())
                        .addValue("sidoCode", row.getSidoCd())
                        .addValue("sigunguCode", row.getSggCd())
                        .addValue("umdCode", row.getUmdCd())
                        .addValue("riCode", row.getRiCd())
                        .addValue("regionName",row.getLocataddNm()))
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql,params);
    }
}

