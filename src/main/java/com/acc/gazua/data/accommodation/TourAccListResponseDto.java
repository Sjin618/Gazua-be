package com.acc.gazua.data.accommodation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) //선언하지 않은 필드는 무시
public class TourAccListResponseDto {

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
        private Integer numOfRows;
        private Integer pageNo;
        private Integer totalCount;
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
        private String addr1;
        private String addr2;
        private String areacode;
        private String sigungucode;
        private String contentid;
        private String contenttypeid;
        private String firstimage;
        private String firstimage2;
        private String mapx;
        private String mapy;
        private String title;
        private String cat3;

        @Override
        public String toString() {
            return "addr1:" + addr1
                    + "addr2:" + addr2
                    + "areaCode:" + areacode
                    + "sigungucCode:" + sigungucode
                    + "contentId:" + contentid
                    + "contentTypeId:" + contenttypeid
                    + "firstImage:" + firstimage
                    + "firstImage2:" + firstimage2
                    + "mapx:" + mapx
                    + "mapy:" + mapy
                    + "title:" + title;
        }
    }

}
