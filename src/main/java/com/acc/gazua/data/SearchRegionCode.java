package com.acc.gazua.data;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchRegionCode {
    private final String LINK = "https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList";

    private final Integer NUM_OF_ROWS = 10;

    private final String TYPE = "json";

    @Value("${SERVICE_KEY}")
    private String serviceKey;

    private final ObjectMapper objectMapper;

    public List<RegionCodeResponse.Row> searchRegionCode(Integer pageNo){
        URI uri = UriComponentsBuilder.fromUriString(LINK)
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("pageNo", pageNo)
                .queryParam("type", TYPE)
                .queryParam("ServiceKey", serviceKey)
                .build()
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        String data = restTemplate.getForObject(uri, String.class);
        RegionCodeResponse regionCodeResponse = objectMapper.readValue(data, RegionCodeResponse.class);
        return regionCodeResponse.getRows();
    }
}
