package com.survey.service;

import com.survey.dto.DashboardOverviewResponse;
import com.survey.dto.SurveyAudienceResponse;
import com.survey.dto.SurveyDashboardResponse;
import com.survey.entity.ResponseSession;
import com.survey.entity.ResponseStatus;
import com.survey.entity.Survey;
import com.survey.repository.ResponseSessionRepository;
import com.survey.repository.SurveyRepository;
import com.survey.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SurveyRepository surveyRepository;
    private final ResponseSessionRepository responseSessionRepository;
    private final VoteRepository voteRepository;

    public DashboardService(SurveyRepository surveyRepository,
                            ResponseSessionRepository responseSessionRepository,
                            VoteRepository voteRepository) {
        this.surveyRepository = surveyRepository;
        this.responseSessionRepository = responseSessionRepository;
        this.voteRepository = voteRepository;
    }

    public DashboardOverviewResponse getOverview() {
        long totalSurveys = surveyRepository.count();
        long activeSurveys = surveyRepository.countByAtivoTrue();
        long inactiveSurveys = Math.max(0, totalSurveys - activeSurveys);

        long totalResponses = responseSessionRepository.count();
        LocalDateTime now = LocalDateTime.now();
        long responses7 = responseSessionRepository.countByCreatedAtAfter(now.minusDays(7));
        long responses30 = responseSessionRepository.countByCreatedAtAfter(now.minusDays(30));

        long completed = responseSessionRepository.countByStatus(ResponseStatus.COMPLETED);
        long abandoned = responseSessionRepository.countByStatus(ResponseStatus.ABANDONED);
        double completionRate = totalResponses == 0 ? 0 : (double) completed / totalResponses;
        double abandonmentRate = totalResponses == 0 ? 0 : (double) abandoned / totalResponses;

        Double avgSeconds = responseSessionRepository.averageCompletionSeconds();
        double avgResponseTime = avgSeconds != null ? avgSeconds : 0;

        List<ResponseSessionRepository.SurveyAggregate> aggregates =
                responseSessionRepository.aggregateBySurvey();
        Map<Long, Survey> surveyMap = surveyRepository.findAllById(
                        aggregates.stream().map(ResponseSessionRepository.SurveyAggregate::getSurveyId).toList())
                .stream()
                .collect(Collectors.toMap(Survey::getId, s -> s));

        List<DashboardOverviewResponse.SurveyMetric> mostResponded = aggregates.stream()
                .sorted(Comparator.comparingLong(ResponseSessionRepository.SurveyAggregate::getTotal).reversed())
                .limit(5)
                .map(agg -> toMetric(agg.getSurveyId(), surveyMap, agg.getTotal()))
                .collect(Collectors.toList());

        List<DashboardOverviewResponse.SurveyMetric> highestCompletion = aggregates.stream()
                .filter(agg -> agg.getTotal() > 0)
                .sorted((a, b) -> Double.compare(rate(b.getCompleted(), b.getTotal()),
                        rate(a.getCompleted(), a.getTotal())))
                .limit(5)
                .map(agg -> toMetric(agg.getSurveyId(), surveyMap, rate(agg.getCompleted(), agg.getTotal())))
                .collect(Collectors.toList());

        List<DashboardOverviewResponse.SurveyMetric> highestAbandonment = aggregates.stream()
                .filter(agg -> agg.getTotal() > 0)
                .sorted((a, b) -> Double.compare(rate(b.getAbandoned(), b.getTotal()),
                        rate(a.getAbandoned(), a.getTotal())))
                .limit(5)
                .map(agg -> toMetric(agg.getSurveyId(), surveyMap, rate(agg.getAbandoned(), agg.getTotal())))
                .collect(Collectors.toList());

        List<DashboardOverviewResponse.SurveySummary> recentSurveys = surveyRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        List<DashboardOverviewResponse.SurveySummary> nearExpiration = surveyRepository
                .findTop5ByDataValidadeAfterOrderByDataValidadeAsc(now)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        DashboardOverviewResponse.Totals totals = new DashboardOverviewResponse.Totals(
                totalSurveys, activeSurveys, inactiveSurveys, totalResponses
        );
        DashboardOverviewResponse.Growth growth = new DashboardOverviewResponse.Growth(responses7, responses30);
        DashboardOverviewResponse.Rates rates = new DashboardOverviewResponse.Rates(completionRate, abandonmentRate);
        DashboardOverviewResponse.Rankings rankings = new DashboardOverviewResponse.Rankings(
                mostResponded,
                highestCompletion,
                highestAbandonment,
                recentSurveys,
                nearExpiration
        );

        return new DashboardOverviewResponse(totals, growth, rates, avgResponseTime, rankings);
    }

    public SurveyDashboardResponse getSurveyDashboard(Long surveyId,
                                                      LocalDateTime from,
                                                      LocalDateTime to) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Pesquisa não encontrada"));

        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(30);
        if (start.isAfter(end)) {
            LocalDateTime tmp = start;
            start = end.minusDays(30);
            end = tmp;
        }

        List<ResponseSession> sessions = responseSessionRepository
                .findBySurveyIdAndCreatedAtBetween(surveyId, start, end);

        long totalResponses = sessions.size();
        long completed = sessions.stream().filter(s -> s.getStatus() == ResponseStatus.COMPLETED).count();
        long abandoned = sessions.stream().filter(s -> s.getStatus() == ResponseStatus.ABANDONED).count();
        double completionRate = totalResponses == 0 ? 0 : (double) completed / totalResponses;
        double abandonmentRate = totalResponses == 0 ? 0 : (double) abandoned / totalResponses;
        double averageResponseTime = sessions.stream()
                .filter(s -> s.getStartedAt() != null && s.getCompletedAt() != null)
                .mapToDouble(s -> java.time.Duration.between(s.getStartedAt(), s.getCompletedAt()).toSeconds())
                .average()
                .orElse(0);

        Map<Long, Long> abandonmentByQuestion = sessions.stream()
                .filter(s -> s.getStatus() == ResponseStatus.ABANDONED && s.getQuestion() != null)
                .collect(Collectors.groupingBy(s -> s.getQuestion().getId(), Collectors.counting()));
        String mostAbandonedQuestion = abandonmentByQuestion.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey())
                .flatMap(id -> sessions.stream()
                        .map(ResponseSession::getQuestion)
                        .filter(q -> q != null && q.getId().equals(id))
                        .map(q -> q.getTexto())
                        .findFirst())
                .orElse(null);

        Map<String, Long> deviceCounts = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getDeviceType()), Collectors.counting()));
        String predominantDevice = deviceCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        SurveyDashboardResponse.Overview overview = new SurveyDashboardResponse.Overview(
                totalResponses,
                completionRate,
                abandonmentRate,
                averageResponseTime,
                mostAbandonedQuestion,
                predominantDevice
        );

        List<SurveyDashboardResponse.QuestionStats> questionStats = voteRepository.aggregateBySurvey(surveyId)
                .stream()
                .collect(Collectors.groupingBy(VoteRepository.QuestionOptionCount::getQuestionId))
                .entrySet()
                .stream()
                .map(entry -> {
                    Long questionId = entry.getKey();
                    List<VoteRepository.QuestionOptionCount> options = entry.getValue();
                    long questionTotal = options.stream().mapToLong(VoteRepository.QuestionOptionCount::getTotal).sum();
                    List<SurveyDashboardResponse.OptionStats> optionStats = options.stream()
                            .map(opt -> new SurveyDashboardResponse.OptionStats(
                                    opt.getOptionId(),
                                    opt.getOptionText(),
                                    opt.getTotal(),
                                    questionTotal == 0 ? 0 : (double) opt.getTotal() / questionTotal
                            ))
                            .sorted(Comparator.comparingLong(SurveyDashboardResponse.OptionStats::getTotal).reversed())
                            .collect(Collectors.toList());
                    String questionText = options.stream()
                            .findFirst()
                            .map(VoteRepository.QuestionOptionCount::getQuestionText)
                            .orElse("Pergunta " + questionId);
                    return new SurveyDashboardResponse.QuestionStats(questionId, questionText, optionStats);
                })
                .collect(Collectors.toList());

        Map<LocalDate, Long> daily = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<SurveyDashboardResponse.DayPoint> dailySeries = daily.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SurveyDashboardResponse.DayPoint(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        Map<Integer, Long> hourly = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().getHour(), Collectors.counting()));
        List<SurveyDashboardResponse.HourPoint> hourlySeries = hourly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SurveyDashboardResponse.HourPoint(String.format("%02d:00", entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());

        SurveyDashboardResponse.TimeSeries timeSeries = new SurveyDashboardResponse.TimeSeries(dailySeries, hourlySeries);

        Map<String, Long> osCounts = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getOperatingSystem()), Collectors.counting()));
        Map<String, Long> browserCounts = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getBrowser()), Collectors.counting()));
        Map<String, Long> sourceCounts = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getSource()), Collectors.counting()));
        Map<String, Long> countryCounts = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getCountry()), Collectors.counting()));
        Map<String, Long> stateCounts = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getState()), Collectors.counting()));
        Map<String, Long> cityCounts = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getCity()), Collectors.counting()));

        SurveyDashboardResponse.Audience audience = new SurveyDashboardResponse.Audience(
                deviceCounts,
                osCounts,
                browserCounts,
                sourceCounts,
                countryCounts,
                stateCounts,
                cityCounts
        );

        // Feature not available due to single-choice votes today
        List<String> limitations = List.of("Combinações de alternativas ainda não estão disponíveis para perguntas de múltipla escolha.");

        return new SurveyDashboardResponse(overview, questionStats, timeSeries, audience, limitations);
    }

    private DashboardOverviewResponse.SurveyMetric toMetric(Long surveyId,
                                                            Map<Long, Survey> surveyMap,
                                                            double value) {
        Survey survey = surveyMap.get(surveyId);
        String title = survey != null ? survey.getTitulo() : "Pesquisa " + surveyId;
        return new DashboardOverviewResponse.SurveyMetric(surveyId, title, value);
    }

    private DashboardOverviewResponse.SurveySummary toSummary(Survey survey) {
        return new DashboardOverviewResponse.SurveySummary(
                survey.getId(),
                survey.getTitulo(),
                survey.getCreatedAt(),
                survey.getDataValidade()
        );
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return (double) numerator / denominator;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    public SurveyAudienceResponse getSurveyAudience(Long surveyId,
                                                    LocalDateTime from,
                                                    LocalDateTime to) {
        surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Pesquisa não encontrada"));

        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(30);
        if (start.isAfter(end)) {
            LocalDateTime tmp = start;
            start = end.minusDays(30);
            end = tmp;
        }

        List<ResponseSession> sessions = responseSessionRepository
                .findBySurveyIdAndCreatedAtBetween(surveyId, start, end);

        Map<String, Long> devices = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getDeviceType()), Collectors.counting()));
        Map<String, Long> os = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getOperatingSystem()), Collectors.counting()));
        Map<String, Long> browsers = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getBrowser()), Collectors.counting()));
        Map<String, Long> sources = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getSource()), Collectors.counting()));
        Map<String, Long> countries = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getCountry()), Collectors.counting()));
        Map<String, Long> states = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getState()), Collectors.counting()));
        Map<String, Long> cities = sessions.stream()
                .collect(Collectors.groupingBy(s -> normalize(s.getCity()), Collectors.counting()));

        List<SurveyAudienceResponse.CategoryValue> peakHours = sessions.stream()
                .collect(Collectors.groupingBy(s -> String.format("%02d:00", s.getCreatedAt().getHour()), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new SurveyAudienceResponse.CategoryValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        List<SurveyAudienceResponse.CategoryValue> peakDays = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().getDayOfWeek().name(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(7)
                .map(entry -> new SurveyAudienceResponse.CategoryValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        double avgAbandonSeconds = sessions.stream()
                .filter(s -> s.getStatus() == ResponseStatus.ABANDONED && s.getStartedAt() != null)
                .mapToDouble(s -> {
                    LocalDateTime endTime = s.getCompletedAt() != null ? s.getCompletedAt() : s.getCreatedAt();
                    if (endTime == null) {
                        endTime = s.getStartedAt();
                    }
                    return java.time.Duration.between(s.getStartedAt(), endTime).toSeconds();
                })
                .average()
                .orElse(0);

        long uniqueRespondents = sessions.stream()
                .map(s -> normalize(s.getIpAddress()))
                .distinct()
                .count();
        long duplicateResponses = Math.max(0, sessions.size() - uniqueRespondents);

        long fastResponses = sessions.stream()
                .filter(s -> s.getStartedAt() != null && s.getCompletedAt() != null)
                .mapToLong(s -> java.time.Duration.between(s.getStartedAt(), s.getCompletedAt()).toSeconds())
                .filter(seconds -> seconds < 5)
                .count();
        List<String> suspicious = fastResponses > 0
                ? List.of("Foram detectadas " + fastResponses + " respostas com tempo inferior a 5 segundos.")
                : List.of();

        return new SurveyAudienceResponse(
                devices,
                os,
                browsers,
                sources,
                countries,
                states,
                cities,
                peakHours,
                peakDays,
                avgAbandonSeconds,
                uniqueRespondents,
                duplicateResponses,
                suspicious
        );
    }
}
