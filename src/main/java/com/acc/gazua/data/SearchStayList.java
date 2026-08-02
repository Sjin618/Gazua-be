package com.acc.gazua.data;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;

@Component
public class SearchStayList {

    private final String LINK = "http://apis.data.go.kr/B551011/KorService2/searchStay2";

    private final String MOBILE_OS = "ETC";
    private final String MOBILE_APP = "Gazua";
    private final String ARRANGE = "A";
    private final int NUM_OF_ROWS = 12;

    @Value("${SERVICE_KEY}")
    private String serviceKey;

    private final ObjectMapper objectMapper;

    public SearchStayList(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    public List<TourAccListResponseDto.Item> searchAccommodation(int pageNo){
        URI uri = UriComponentsBuilder.fromUriString(LINK)
                .queryParam("MobileOS",MOBILE_OS)
                .queryParam("MobileApp",MOBILE_APP)
                .queryParam("arrange",ARRANGE)
                .queryParam("numOfRows",NUM_OF_ROWS)
                .queryParam("_type","json")
                .queryParam("ServiceKey",serviceKey)
                .queryParam("pageNo",pageNo)
                .build(true)
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(uri, String.class);
        TourAccListResponseDto acc = objectMapper.readValue(response, TourAccListResponseDto.class);
        return acc.getResponse().getBody().getItems().getItem();
    }


}
