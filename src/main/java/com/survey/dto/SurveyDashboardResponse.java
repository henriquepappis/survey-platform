package com.survey.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SurveyDashboardResponse {

    private Overview overview;
    private List<QuestionStats> questions;
    private TimeSeries timeSeries;
    private Audience audience;
    private List<String> limitations;

    public SurveyDashboardResponse(Overview overview,
                                   List<QuestionStats> questions,
                                   TimeSeries timeSeries,
                                   Audience audience,
                                   List<String> limitations) {
        this.overview = overview;
        this.questions = questions;
        this.timeSeries = timeSeries;
        this.audience = audience;
        this.limitations = limitations;
    }

    public Overview getOverview() {
        return overview;
    }

    public List<QuestionStats> getQuestions() {
        return questions;
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    public Audience getAudience() {
        return audience;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public static class Overview {
        private long totalResponses;
        private double completionRate;
        private double abandonmentRate;
        private double averageResponseTimeSeconds;
        private String mostAbandonedQuestion;
        private String predominantDevice;

        public Overview(long totalResponses,
                        double completionRate,
                        double abandonmentRate,
                        double averageResponseTimeSeconds,
                        String mostAbandonedQuestion,
                        String predominantDevice) {
            this.totalResponses = totalResponses;
            this.completionRate = completionRate;
            this.abandonmentRate = abandonmentRate;
            this.averageResponseTimeSeconds = averageResponseTimeSeconds;
            this.mostAbandonedQuestion = mostAbandonedQuestion;
            this.predominantDevice = predominantDevice;
        }

        public long getTotalResponses() {
            return totalResponses;
        }

        public double getCompletionRate() {
            return completionRate;
        }

        public double getAbandonmentRate() {
            return abandonmentRate;
        }

        public double getAverageResponseTimeSeconds() {
            return averageResponseTimeSeconds;
        }

        public String getMostAbandonedQuestion() {
            return mostAbandonedQuestion;
        }

        public String getPredominantDevice() {
            return predominantDevice;
        }
    }

    public static class QuestionStats {
        private Long questionId;
        private String questionText;
        private List<OptionStats> options;

        public QuestionStats(Long questionId, String questionText, List<OptionStats> options) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.options = options;
        }

        public Long getQuestionId() {
            return questionId;
        }

        public String getQuestionText() {
            return questionText;
        }

        public List<OptionStats> getOptions() {
            return options;
        }
    }

    public static class OptionStats {
        private Long optionId;
        private String optionText;
        private long total;
        private double percentage;

        public OptionStats(Long optionId, String optionText, long total, double percentage) {
            this.optionId = optionId;
            this.optionText = optionText;
            this.total = total;
            this.percentage = percentage;
        }

        public Long getOptionId() {
            return optionId;
        }

        public String getOptionText() {
            return optionText;
        }

        public long getTotal() {
            return total;
        }

        public double getPercentage() {
            return percentage;
        }
    }

    public static class TimeSeries {
        private List<DayPoint> daily;
        private List<HourPoint> hourly;

        public TimeSeries(List<DayPoint> daily, List<HourPoint> hourly) {
            this.daily = daily;
            this.hourly = hourly;
        }

        public List<DayPoint> getDaily() {
            return daily;
        }

        public List<HourPoint> getHourly() {
            return hourly;
        }
    }

    public static class DayPoint {
        private LocalDate date;
        private long total;

        public DayPoint(LocalDate date, long total) {
            this.date = date;
            this.total = total;
        }

        public LocalDate getDate() {
            return date;
        }

        public long getTotal() {
            return total;
        }
    }

    public static class HourPoint {
        private String hour;
        private long total;

        public HourPoint(String hour, long total) {
            this.hour = hour;
            this.total = total;
        }

        public String getHour() {
            return hour;
        }

        public long getTotal() {
            return total;
        }
    }

    public static class Audience {
        private Map<String, Long> devices;
        private Map<String, Long> operatingSystems;
        private Map<String, Long> browsers;
        private Map<String, Long> sources;
        private Map<String, Long> countries;
        private Map<String, Long> states;
        private Map<String, Long> cities;

        public Audience(Map<String, Long> devices,
                        Map<String, Long> operatingSystems,
                        Map<String, Long> browsers,
                        Map<String, Long> sources,
                        Map<String, Long> countries,
                        Map<String, Long> states,
                        Map<String, Long> cities) {
            this.devices = devices;
            this.operatingSystems = operatingSystems;
            this.browsers = browsers;
            this.sources = sources;
            this.countries = countries;
            this.states = states;
            this.cities = cities;
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
    }
}
