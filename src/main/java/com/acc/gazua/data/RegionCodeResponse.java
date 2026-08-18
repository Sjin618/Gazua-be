package com.acc.gazua.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
public class RegionCodeResponse {
    @JsonProperty("StanReginCd")
    private List<RegionCodeElement> stanReginCd;

    public List<Row> getRows(){
        return stanReginCd.stream()
                .map(RegionCodeElement::getRow)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RegionCodeElement{
        private List<Head> head;
        private List<Row> row;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Head{
        private Integer totalCount;
        private String numOfRows;
        private String pageNo;
        private String type;

        @JsonProperty("RESULT")
        private Result result;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result{
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row{
        //"region_cd":"2717010900","sido_cd":"27","sgg_cd":"170","umd_cd":"109","ri_cd":"00","locatjumin_cd":"2717010900","locatjijuk_cd":"2717010900","locatadd_nm":"대구광역시 서구 원대동3가","locat_order":9,"locat_rm":"","locathigh_cd":"2717000000","locallow_nm":"원대동3가","adpt_de":""
        @JsonProperty("region_cd")
        private String regionCd;

        @JsonProperty("sido_cd")
        private String sidoCd;

        @JsonProperty("sgg_cd")
        private String sggCd;

        @JsonProperty("umd_cd")
        private String umdCd;

        @JsonProperty("ri_cd")
        private String riCd;

        @JsonProperty("locatjumin_cd")
        private String locatjuminCd;

        @JsonProperty("locatjijuk_cd")
        private String locatjijukCd;

        @JsonProperty("locatadd_nm")
        private String locataddNm;

        @JsonProperty("locat_order")
        private String locatOrder;

        @JsonProperty("locat_rm")
        private String locatRm;

        @JsonProperty("locathigh_cd")
        private String locathighCd;

        @JsonProperty("locallow_nm")
        private String locallowNm;

        @JsonProperty("adpt_de")
        private String adptDe;
    }


}
