
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
    "htmlValue",
    "optinValue",
    "textValue",
    "insertOnNoMatch",
    "updateOnMatch",
    "matchColumnName1",
    "matchColumnName2",
    "matchOperator",
    "optoutValue",
    "rejectRecordIfChannelEmpty",
    "defaultPermissionStatus"
})
public class MergeRule {

    @JsonProperty("htmlValue")
    private String htmlValue = "H";
    @JsonProperty("optinValue")
    private String optinValue = "Y";
    @JsonProperty("textValue")
    private String textValue = "T";
    @JsonProperty("insertOnNoMatch")
    private Boolean insertOnNoMatch = true;
    @JsonProperty("updateOnMatch")
    private String updateOnMatch = "REPLACE_ALL";
    @JsonProperty("matchColumnName1")
    private String matchColumnName1 = "EMAIL_ADDRESS_";
    @JsonProperty("matchColumnName2")
    private Object matchColumnName2 = null;
    @JsonProperty("matchOperator")
    private String matchOperator = "NONE";
    @JsonProperty("optoutValue")
    private String optoutValue = "N";
    @JsonProperty("rejectRecordIfChannelEmpty")
    private Object rejectRecordIfChannelEmpty = null;
    @JsonProperty("defaultPermissionStatus")
    private String defaultPermissionStatus = "OPTIN";
    
    @JsonProperty("htmlValue")
    public String getHtmlValue() {
        return htmlValue;
    }

    @JsonProperty("htmlValue")
    public void setHtmlValue(String htmlValue) {
        this.htmlValue = htmlValue;
    }

    @JsonProperty("optinValue")
    public String getOptinValue() {
        return optinValue;
    }

    @JsonProperty("optinValue")
    public void setOptinValue(String optinValue) {
        this.optinValue = optinValue;
    }

    @JsonProperty("textValue")
    public String getTextValue() {
        return textValue;
    }

    @JsonProperty("textValue")
    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    @JsonProperty("insertOnNoMatch")
    public Boolean getInsertOnNoMatch() {
        return insertOnNoMatch;
    }

    @JsonProperty("insertOnNoMatch")
    public void setInsertOnNoMatch(Boolean insertOnNoMatch) {
        this.insertOnNoMatch = insertOnNoMatch;
    }

    @JsonProperty("updateOnMatch")
    public String getUpdateOnMatch() {
        return updateOnMatch;
    }

    @JsonProperty("updateOnMatch")
    public void setUpdateOnMatch(String updateOnMatch) {
        this.updateOnMatch = updateOnMatch;
    }

    @JsonProperty("matchColumnName1")
    public String getMatchColumnName1() {
        return matchColumnName1;
    }

    @JsonProperty("matchColumnName1")
    public void setMatchColumnName1(String matchColumnName1) {
        this.matchColumnName1 = matchColumnName1;
    }

    @JsonProperty("matchColumnName2")
    public Object getMatchColumnName2() {
        return matchColumnName2;
    }

    @JsonProperty("matchColumnName2")
    public void setMatchColumnName2(Object matchColumnName2) {
        this.matchColumnName2 = matchColumnName2;
    }

    @JsonProperty("matchOperator")
    public String getMatchOperator() {
        return matchOperator;
    }

    @JsonProperty("matchOperator")
    public void setMatchOperator(String matchOperator) {
        this.matchOperator = matchOperator;
    }

    @JsonProperty("optoutValue")
    public String getOptoutValue() {
        return optoutValue;
    }

    @JsonProperty("optoutValue")
    public void setOptoutValue(String optoutValue) {
        this.optoutValue = optoutValue;
    }

    @JsonProperty("rejectRecordIfChannelEmpty")
    public Object getRejectRecordIfChannelEmpty() {
        return rejectRecordIfChannelEmpty;
    }

    @JsonProperty("rejectRecordIfChannelEmpty")
    public void setRejectRecordIfChannelEmpty(Object rejectRecordIfChannelEmpty) {
        this.rejectRecordIfChannelEmpty = rejectRecordIfChannelEmpty;
    }

    @JsonProperty("defaultPermissionStatus")
    public String getDefaultPermissionStatus() {
        return defaultPermissionStatus;
    }

    @JsonProperty("defaultPermissionStatus")
    public void setDefaultPermissionStatus(String defaultPermissionStatus) {
        this.defaultPermissionStatus = defaultPermissionStatus;
    }

}
