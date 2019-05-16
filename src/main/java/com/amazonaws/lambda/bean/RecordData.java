
package com.amazonaws.lambda.bean;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "fieldNames",
    "records",
    "mapTemplateName"
})
public class RecordData {

    @JsonProperty("fieldNames")
    private List<String> fieldNames = null;
    @JsonProperty("records")
    private List<List<String>> records = null;
    @JsonProperty("mapTemplateName")
    private Object mapTemplateName = null;
    
    @JsonProperty("fieldNames")
    public List<String> getFieldNames() {
        return fieldNames;
    }

    @JsonProperty("fieldNames")
    public void setFieldNames(List<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    @JsonProperty("records")
    public List<List<String>> getRecords() {
        return records;
    }

    @JsonProperty("records")
    public void setRecords(List<List<String>> records) {
        this.records = records;
    }

    @JsonProperty("mapTemplateName")
    public Object getMapTemplateName() {
        return mapTemplateName;
    }

    @JsonProperty("mapTemplateName")
    public void setMapTemplateName(Object mapTemplateName) {
        this.mapTemplateName = mapTemplateName;
    }

}
