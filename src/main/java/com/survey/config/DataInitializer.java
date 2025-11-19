package com.survey.config;

import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.SurveyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional
public class DataInitializer implements CommandLineRunner {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final boolean initializeData;

    public DataInitializer(SurveyRepository surveyRepository,
                           QuestionRepository questionRepository,
                           OptionRepository optionRepository,
                           @Value("${app.data.initialize:true}") boolean initializeData) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.initializeData = initializeData;
    }

    @Override
    public void run(String... args) {
        if (!initializeData || surveyRepository.count() > 0) {
            return;
        }

        Survey survey = new Survey();
        survey.setTitulo("Pesquisa de Satisfação");
        survey.setAtivo(true);
        survey.setDataValidade(LocalDateTime.now().plusMonths(1));
        Survey savedSurvey = surveyRepository.save(survey);

        Question question1 = createQuestion(savedSurvey, "Qual o seu nível de satisfação geral?", 1);
        Question question2 = createQuestion(savedSurvey, "Você recomendaria nosso produto?", 2);

        Question savedQuestion1 = questionRepository.save(question1);
        Question savedQuestion2 = questionRepository.save(question2);

        optionRepository.save(createOption(savedQuestion1, "Muito satisfeito", true));
        optionRepository.save(createOption(savedQuestion1, "Satisfeito", true));
        optionRepository.save(createOption(savedQuestion1, "Neutro", true));
        optionRepository.save(createOption(savedQuestion1, "Insatisfeito", true));
        optionRepository.save(createOption(savedQuestion2, "Sim", true));
        optionRepository.save(createOption(savedQuestion2, "Não", true));
    }

    private Question createQuestion(Survey survey, String texto, int ordem) {
        Question question = new Question();
        question.setSurvey(survey);
        question.setTexto(texto);
        question.setOrdem(ordem);
        return question;
    }

    private Option createOption(Question question, String texto, boolean ativo) {
        Option option = new Option();
        option.setQuestion(question);
        option.setTexto(texto);
        option.setAtivo(ativo);
        return option;
    }
}
