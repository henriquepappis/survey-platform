package com.survey.dto;

import java.util.List;
import java.util.Map;

public class SurveyAudienceResponse {

    private Map<String, Long> devices;
    private Map<String, Long> operatingSystems;
    private Map<String, Long> browsers;
    private Map<String, Long> sources;
    private Map<String, Long> countries;
    private Map<String, Long> states;
    private Map<String, Long> cities;
    private List<CategoryValue> peakHours;
    private List<CategoryValue> peakDays;
    private double averageAbandonmentTimeSeconds;
    private long uniqueRespondents;
    private long duplicateResponses;
    private List<String> suspiciousIndicators;

    public SurveyAudienceResponse(Map<String, Long> devices,
                                  Map<String, Long> operatingSystems,
                                  Map<String, Long> browsers,
                                  Map<String, Long> sources,
                                  Map<String, Long> countries,
                                  Map<String, Long> states,
                                  Map<String, Long> cities,
                                  List<CategoryValue> peakHours,
                                  List<CategoryValue> peakDays,
                                  double averageAbandonmentTimeSeconds,
                                  long uniqueRespondents,
                                  long duplicateResponses,
                                  List<String> suspiciousIndicators) {
        this.devices = devices;
        this.operatingSystems = operatingSystems;
        this.browsers = browsers;
        this.sources = sources;
        this.countries = countries;
        this.states = states;
        this.cities = cities;
        this.peakHours = peakHours;
        this.peakDays = peakDays;
        this.averageAbandonmentTimeSeconds = averageAbandonmentTimeSeconds;
        this.uniqueRespondents = uniqueRespondents;
        this.duplicateResponses = duplicateResponses;
        this.suspiciousIndicators = suspiciousIndicators;
    }

    public Map<String, Long> getDevices() {
        return devices;
    }

    public Map<String, Long> getOperatingSystems() {
        return operatingSystems;
    }

    public Map<String, Long> getBrowsers() {
        return browsers;
    }

    public Map<String, Long> getSources() {
        return sources;
    }

    public Map<String, Long> getCountries() {
        return countries;
    }

    public Map<String, Long> getStates() {
        return states;
    }

    public Map<String, Long> getCities() {
        return cities;
    }

    public List<CategoryValue> getPeakHours() {
        return peakHours;
    }

    public List<CategoryValue> getPeakDays() {
        return peakDays;
    }

    public double getAverageAbandonmentTimeSeconds() {
        return averageAbandonmentTimeSeconds;
    }

    public long getUniqueRespondents() {
        return uniqueRespondents;
    }

    public long getDuplicateResponses() {
        return duplicateResponses;
    }

    public List<String> getSuspiciousIndicators() {
        return suspiciousIndicators;
    }

    public static class CategoryValue {
        private String category;
        private long value;

        public CategoryValue(String category, long value) {
            this.category = category;
            this.value = value;
        }

        public String getCategory() {
            return category;
        }

        public long getValue() {
            return value;
        }
    }
}
