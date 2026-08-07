package com.acc.gazua.data;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;

@Component
public class SearchRoom {
    private final String LINK = "https://apis.data.go.kr/B551011/KorService2/detailInfo2";
    private final String MOBILE_OS = "ETC";
    private final String MOBILE_APP = "Gazua";

    @Value("${SERVICE_KEY}")
    private String serviceKey;

    private final ObjectMapper tourObjectMapper;

    public SearchRoom(@Qualifier("tourObjectMapper") ObjectMapper tourObjectMapper){
        this.tourObjectMapper = tourObjectMapper;
    }

    public List<TourRoomResponseDto.Item> searchRoom(String contentTypeId,
                                                     String contentId){
        URI uri = UriComponentsBuilder.fromUriString(LINK)
                .queryParam("ServiceKey", serviceKey)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("contentId", contentId)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type","json")
                .build(true)
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        String data = restTemplate.getForObject(uri, String.class);
        TourRoomResponseDto responseDto = tourObjectMapper.readValue(data, TourRoomResponseDto.class);

        if(responseDto.getResponse().getBody().getItems() == null) return null;
        return responseDto.getResponse().getBody().getItems().getItem();
    }
}
