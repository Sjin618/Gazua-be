package com.acc.gazua.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourAccResponseDto {
    private Response response;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response{
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body{
        private Items items;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items{
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item{
        private String overview;
        private String lDongRegnCd;
        private String lDongSignguCd;
    }
}
