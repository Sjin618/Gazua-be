package com.acc.gazua.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourRoomResponseDto {
    private Response response;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response{
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header{
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body{
        private Items items;
        private String numOfRows;
        private String pageNo;
        private String totalCount;
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
        private String roomtitle;
        private String roomcount;
        private String roombasecount;
        private String roommaxcount;
        private String roomoffseasonminfee1; //주중최소 비수기
        private String roomoffseasonminfee2; //주말최소 비수기
        private String roompeakseasonminfee1; //주중최소 성수기
        private String roompeakseasonminfee2; //주말최소 성수기
        /* 편의시설 */
        private String roombathfacility;
        private String roombath;
        private String roomhometheater;
        private String roomaircondition;
        private String roomtv;
        private String roompc;
        private String roomcable;
        private String roominternet;
        private String roomrefrigerator;
        private String roomtoiletries;
        private String roomsofa;
        private String roomcook;
        private String roomtable;
        private String roomhairdryer;

        //방 이미지
        private String roomimg1;
        private String roomimg2;
        private String roomimg3;
        private String roomimg4;
        private String roomimg5;
    }
}
