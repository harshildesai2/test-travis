
package com.amazonaws.lambda.bean;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "recordData",
    "mergeRule"
})
public class SubscriptionReq {

    @JsonProperty("recordData")
    private RecordData recordData;
    @JsonProperty("mergeRule")
    private MergeRule mergeRule;
    
    @JsonProperty("recordData")
    public RecordData getRecordData() {
        return recordData;
    }

    @JsonProperty("recordData")
    public void setRecordData(RecordData recordData) {
        this.recordData = recordData;
    }

    @JsonProperty("mergeRule")
    public MergeRule getMergeRule() {
        return mergeRule;
    }

    @JsonProperty("mergeRule")
    public void setMergeRule(MergeRule mergeRule) {
        this.mergeRule = mergeRule;
    }

}
