package com.acc.gazua.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Component
public class SearchStay {
    private final String LINK = "http://apis.data.go.kr/B551011/KorService2/detailCommon2";

    private final String MOBILE_OS = "ETC";
    private final String MOBILE_APP = "Gazua";
    @Value("${SERVICE_KEY}")
    private String serviceKey;

    private final ObjectMapper objectMapper;

    public SearchStay(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String searchStayDescription(String contentId){
        URI uri = UriComponentsBuilder.fromUriString(LINK)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("ServiceKey", serviceKey)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .build(true)
                .toUri();
        RestTemplate restTemplate = new RestTemplate();
        String data = restTemplate.getForObject(uri, String.class);
        TourAccResponseDto dto = objectMapper.readValue(data, TourAccResponseDto.class);
        return dto.getResponse().getBody().getItems().getItem().get(0).getOverview();
    }
}
