package com.survey.service;

import com.survey.dto.VoteSummaryResponseDTO;
import com.survey.repository.VoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final VoteRepository voteRepository;

    public AnalyticsService(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    public VoteSummaryResponseDTO summarizeVotes(Long surveyId) {
        List<VoteRepository.VoteSummary> summaries = voteRepository.summarizeBySurvey(surveyId);

        Map<Long, List<VoteRepository.VoteSummary>> grouped = summaries.stream()
                .collect(Collectors.groupingBy(VoteRepository.VoteSummary::getQuestionId));

        List<VoteSummaryResponseDTO.QuestionVotes> questions = grouped.entrySet().stream()
                .map(entry -> new VoteSummaryResponseDTO.QuestionVotes(
                        entry.getKey(),
                        entry.getValue().get(0).getQuestionTitulo(),
                        entry.getValue().stream()
                                .map(item -> new VoteSummaryResponseDTO.OptionVotes(
                                        item.getOptionId(),
                                        item.getOptionTitulo(),
                                        item.getTotal()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new VoteSummaryResponseDTO(surveyId, questions);
    }
}
