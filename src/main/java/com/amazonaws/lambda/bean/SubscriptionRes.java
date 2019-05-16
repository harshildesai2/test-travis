
package com.amazonaws.lambda.bean;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "recordData",
    "mergeRule",
    "links"
})
public class SubscriptionRes {

    @JsonProperty("recordData")
    private RecordData recordData;
    @JsonProperty("mergeRule")
    private MergeRule mergeRule;
    @JsonProperty("links")
    private List<Link> links = null;

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

    @JsonProperty("links")
    public List<Link> getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(List<Link> links) {
        this.links = links;
    }

}
