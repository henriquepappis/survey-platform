package com.survey.config;

import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.entity.UserAccount;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.SurveyRepository;
import com.survey.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Transactional
public class DataInitializer implements CommandLineRunner {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean initializeData;

    public DataInitializer(SurveyRepository surveyRepository,
                           QuestionRepository questionRepository,
                           OptionRepository optionRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.data.initialize:false}") boolean initializeData) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initializeData = initializeData;
    }

    @Override
    public void run(String... args) {
        if (!initializeData) {
            return;
        }

        // Seeds desativadas: manter apenas estrutura via migrations
    }
}
