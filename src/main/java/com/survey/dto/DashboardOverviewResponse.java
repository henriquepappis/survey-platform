package com.survey.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DashboardOverviewResponse {

    private Totals totals;
    private Growth growth;
    private Rates rates;
    private double averageResponseTimeSeconds;
    private Rankings rankings;

    public DashboardOverviewResponse(Totals totals,
                                     Growth growth,
                                     Rates rates,
                                     double averageResponseTimeSeconds,
                                     Rankings rankings) {
        this.totals = totals;
        this.growth = growth;
        this.rates = rates;
        this.averageResponseTimeSeconds = averageResponseTimeSeconds;
        this.rankings = rankings;
    }

    public Totals getTotals() {
        return totals;
    }

    public Growth getGrowth() {
        return growth;
    }

    public Rates getRates() {
        return rates;
    }

    public double getAverageResponseTimeSeconds() {
        return averageResponseTimeSeconds;
    }

    public Rankings getRankings() {
        return rankings;
    }

    public static class Totals {
        private long totalSurveys;
        private long activeSurveys;
        private long inactiveSurveys;
        private long totalResponses;

        public Totals(long totalSurveys, long activeSurveys, long inactiveSurveys, long totalResponses) {
            this.totalSurveys = totalSurveys;
            this.activeSurveys = activeSurveys;
            this.inactiveSurveys = inactiveSurveys;
            this.totalResponses = totalResponses;
        }

        public long getTotalSurveys() {
            return totalSurveys;
        }

        public long getActiveSurveys() {
            return activeSurveys;
        }

        public long getInactiveSurveys() {
            return inactiveSurveys;
        }

        public long getTotalResponses() {
            return totalResponses;
        }
    }

    public static class Growth {
        private long responsesLast7Days;
        private long responsesLast30Days;

        public Growth(long responsesLast7Days, long responsesLast30Days) {
            this.responsesLast7Days = responsesLast7Days;
            this.responsesLast30Days = responsesLast30Days;
        }

        public long getResponsesLast7Days() {
            return responsesLast7Days;
        }

        public long getResponsesLast30Days() {
            return responsesLast30Days;
        }
    }

    public static class Rates {
        private double completionRate;
        private double abandonmentRate;

        public Rates(double completionRate, double abandonmentRate) {
            this.completionRate = completionRate;
            this.abandonmentRate = abandonmentRate;
        }

        public double getCompletionRate() {
            return completionRate;
        }

        public double getAbandonmentRate() {
            return abandonmentRate;
        }
    }

    public static class Rankings {
        private List<SurveyMetric> mostResponded;
        private List<SurveyMetric> highestCompletionRate;
        private List<SurveyMetric> highestAbandonmentRate;
        private List<SurveySummary> recentlyCreated;
        private List<SurveySummary> nearExpiration;

        public Rankings(List<SurveyMetric> mostResponded,
                        List<SurveyMetric> highestCompletionRate,
                        List<SurveyMetric> highestAbandonmentRate,
                        List<SurveySummary> recentlyCreated,
                        List<SurveySummary> nearExpiration) {
            this.mostResponded = mostResponded;
            this.highestCompletionRate = highestCompletionRate;
            this.highestAbandonmentRate = highestAbandonmentRate;
            this.recentlyCreated = recentlyCreated;
            this.nearExpiration = nearExpiration;
        }

        public List<SurveyMetric> getMostResponded() {
            return mostResponded;
        }

        public List<SurveyMetric> getHighestCompletionRate() {
            return highestCompletionRate;
        }

        public List<SurveyMetric> getHighestAbandonmentRate() {
            return highestAbandonmentRate;
        }

        public List<SurveySummary> getRecentlyCreated() {
            return recentlyCreated;
        }

        public List<SurveySummary> getNearExpiration() {
            return nearExpiration;
        }
    }

    public static class SurveyMetric {
        private Long surveyId;
        private String title;
        private double value;

        public SurveyMetric(Long surveyId, String title, double value) {
            this.surveyId = surveyId;
            this.title = title;
            this.value = value;
        }

        public Long getSurveyId() {
            return surveyId;
        }

        public String getTitle() {
            return title;
        }

        public double getValue() {
            return value;
        }
    }

    public static class SurveySummary {
        private Long surveyId;
        private String title;
        private LocalDateTime createdAt;
        private LocalDateTime expirationDate;

        public SurveySummary(Long surveyId, String title, LocalDateTime createdAt, LocalDateTime expirationDate) {
            this.surveyId = surveyId;
            this.title = title;
            this.createdAt = createdAt;
            this.expirationDate = expirationDate;
        }

        public Long getSurveyId() {
            return surveyId;
        }

        public String getTitle() {
            return title;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public LocalDateTime getExpirationDate() {
            return expirationDate;
        }
    }
}
