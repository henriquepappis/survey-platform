package com.survey.service;

import com.survey.dto.VoteRequestDTO;
import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.entity.Vote;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.SurveyRepository;
import com.survey.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VoteService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final VoteRepository voteRepository;

    public VoteService(SurveyRepository surveyRepository,
                       QuestionRepository questionRepository,
                       OptionRepository optionRepository,
                       VoteRepository voteRepository) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
    }

    public void registerVote(VoteRequestDTO request, String ipAddress, String userAgent) {
        Survey survey = surveyRepository.findById(request.getSurveyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada"));

        if (Boolean.FALSE.equals(survey.getAtivo())) {
            throw new IllegalStateException("Pesquisa está inativa");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta não encontrada"));
        if (!question.getSurvey().getId().equals(survey.getId())) {
            throw new IllegalStateException("Pergunta não pertence à pesquisa");
        }

        Option option = optionRepository.findById(request.getOptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Opção não encontrada"));
        if (!option.getQuestion().getId().equals(question.getId())) {
            throw new IllegalStateException("Opção não pertence à pergunta");
        }

        Vote vote = new Vote();
        vote.setSurvey(survey);
        vote.setQuestion(question);
        vote.setOption(option);
        vote.setIpAddress(ipAddress);
        vote.setUserAgent(userAgent);

        voteRepository.save(vote);
    }
}
