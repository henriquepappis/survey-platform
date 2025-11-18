package com.survey.service;

import com.survey.dto.VoteSummaryResponseDTO;
import com.survey.repository.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    @Test
    @DisplayName("summarizeVotes deve agrupar por pergunta e opção")
    void summarizeVotes_shouldGroupByQuestion() {
        VoteRepository voteRepository = Mockito.mock(VoteRepository.class);
        AnalyticsService service = new AnalyticsService(voteRepository);

        VoteRepository.VoteSummary summary1 = mockSummary(1L, "Pergunta 1", 10L, "Opção 1", 5L);
        VoteRepository.VoteSummary summary2 = mockSummary(1L, "Pergunta 1", 11L, "Opção 2", 3L);
        VoteRepository.VoteSummary summary3 = mockSummary(2L, "Pergunta 2", 20L, "Opção A", 7L);

        when(voteRepository.summarizeBySurvey(anyLong()))
                .thenReturn(List.of(summary1, summary2, summary3));

        VoteSummaryResponseDTO response = service.summarizeVotes(99L);

        assertThat(response.getSurveyId()).isEqualTo(99L);
        assertThat(response.getQuestions()).hasSize(2);
        assertThat(response.getQuestions().get(0).getOptions()).hasSize(2);
    }

    private VoteRepository.VoteSummary mockSummary(Long questionId, String questionTitulo,
                                                   Long optionId, String optionTitulo,
                                                   Long total) {
        VoteRepository.VoteSummary summary = Mockito.mock(VoteRepository.VoteSummary.class);
        when(summary.getQuestionId()).thenReturn(questionId);
        when(summary.getQuestionTitulo()).thenReturn(questionTitulo);
        when(summary.getOptionId()).thenReturn(optionId);
        when(summary.getOptionTitulo()).thenReturn(optionTitulo);
        when(summary.getTotal()).thenReturn(total);
        return summary;
    }
}
