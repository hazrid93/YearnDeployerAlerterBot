package com.azad.yearn.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XSource_Content {

    @JsonProperty("status")
    private String status;
    @JsonProperty("message")
    private String message;
    @JsonProperty("result")
    private List<Map<String,String>> result = null;

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("result")
    public List<Map<String,String>> getResult() {
        return result;
    }

    @JsonProperty("result")
    public void setResult(List<Map<String,String>> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "XSource_Content{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", result=" + result +
                '}';
    }
}
