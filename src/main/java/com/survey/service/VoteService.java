package com.survey.service;

import com.survey.dto.VoteRequestDTO;
import com.survey.dto.VoteResponseDTO;
import com.survey.entity.*;
import com.survey.exception.BusinessException;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.ResponseSessionRepository;
import com.survey.repository.SurveyRepository;
import com.survey.repository.VoteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class VoteService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final VoteRepository voteRepository;
    private final ResponseSessionRepository responseSessionRepository;
    private final long duplicateWindowMinutes;
    private final boolean anonymizeIp;
    private final boolean audienceEnabled;
    private final Counter duplicateVoteBlockedCounter;

    public VoteService(SurveyRepository surveyRepository,
                       QuestionRepository questionRepository,
                       OptionRepository optionRepository,
                       VoteRepository voteRepository,
                       ResponseSessionRepository responseSessionRepository,
                       @Value("${app.votes.duplicate-window-minutes:10}") long duplicateWindowMinutes,
                       @Value("${app.privacy.ip-anonymize:true}") boolean anonymizeIp,
                       @Value("${app.privacy.audience-enabled:true}") boolean audienceEnabled,
                       MeterRegistry meterRegistry) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
        this.responseSessionRepository = responseSessionRepository;
        this.duplicateWindowMinutes = duplicateWindowMinutes;
        this.anonymizeIp = anonymizeIp;
        this.audienceEnabled = audienceEnabled;
        this.duplicateVoteBlockedCounter = meterRegistry.counter("vote.duplicate.blocked");
    }

    public VoteResponseDTO registerVote(VoteRequestDTO request, String ipAddress, String userAgent) {
        Survey survey = surveyRepository.findById(request.getSurveyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada"));

        if (Boolean.FALSE.equals(survey.getAtivo())) {
            throw new BusinessException("Pesquisa está inativa");
        }
        if (survey.getDataValidade() != null && survey.getDataValidade().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Pesquisa expirada");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta não encontrada"));
        if (!question.getSurvey().getId().equals(survey.getId())) {
            throw new BusinessException("Pergunta não pertence à pesquisa");
        }

        Option option = optionRepository.findById(request.getOptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Opção não encontrada"));
        if (!option.getQuestion().getId().equals(question.getId())) {
            throw new BusinessException("Opção não pertence à pergunta");
        }
        if (Boolean.FALSE.equals(option.getAtivo())) {
            throw new BusinessException("Opção está inativa");
        }

        String safeIp = anonymizeIfNeeded(normalize(ipAddress));
        String safeUserAgent = normalize(userAgent);

        Vote vote = new Vote();
        vote.setSurvey(survey);
        vote.setQuestion(question);
        vote.setOption(option);
        vote.setIpAddress(safeIp);
        vote.setUserAgent(safeUserAgent);
        ResponseSession session = audienceEnabled
                ? buildSession(request, survey, question, ipAddress, userAgent)
                : null;
        if (session != null) {
            responseSessionRepository.save(session);
            vote.setResponseSession(session);
        }

        Vote savedVote = voteRepository.save(vote);
        String antifraudToken = session != null ? "session-" + session.getId() : null;
        Long sessionId = session != null ? session.getId() : null;
        return new VoteResponseDTO(savedVote.getId(), sessionId, antifraudToken);
    }

    private ResponseSession buildSession(VoteRequestDTO request,
                                         Survey survey,
                                         Question question,
                                         String ip,
                                         String userAgent) {
        ResponseSession session = new ResponseSession();
        session.setSurvey(survey);
        session.setQuestion(question);
        session.setIpAddress(ip);
        session.setUserAgent(userAgent);
        session.setDeviceType(firstNonBlank(request.getDeviceType(), detectDevice(userAgent)));
        session.setOperatingSystem(firstNonBlank(request.getOperatingSystem(), detectOperatingSystem(userAgent)));
        session.setBrowser(firstNonBlank(request.getBrowser(), detectBrowser(userAgent)));
        session.setSource(firstNonBlank(request.getSource(), "unknown"));
        session.setCountry(firstNonBlank(request.getCountry(), "unknown"));
        session.setState(firstNonBlank(request.getState(), null));
        session.setCity(firstNonBlank(request.getCity(), null));
        ResponseStatus status = request.getStatus() != null ? request.getStatus() : ResponseStatus.COMPLETED;
        session.setStatus(status);
        session.setStartedAt(request.getStartedAt() != null ? request.getStartedAt() : java.time.LocalDateTime.now());
        if (status == ResponseStatus.COMPLETED) {
            session.setCompletedAt(request.getCompletedAt() != null ? request.getCompletedAt() : java.time.LocalDateTime.now());
        } else if (status == ResponseStatus.ABANDONED) {
            session.setCompletedAt(request.getCompletedAt());
        }
        return session;
    }

    private String firstNonBlank(String candidate, String fallback) {
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        return fallback;
    }

    private String detectDevice(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile")) {
            if (ua.contains("tablet") || ua.contains("ipad")) {
                return "tablet";
            }
            return "mobile";
        }
        return "desktop";
    }

    private String detectOperatingSystem(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os") || ua.contains("macintosh")) return "macOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        return "unknown";
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome")) return "Chrome";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("edge")) return "Edge";
        if (ua.contains("opera") || ua.contains("opr")) return "Opera";
        if (ua.contains("msie") || ua.contains("trident")) return "Internet Explorer";
        return "unknown";
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private String anonymizeIfNeeded(String ip) {
        if (!anonymizeIp) {
            return ip;
        }
        if ("unknown".equals(ip)) {
            return ip;
        }
        // Trunca último octeto (IPv4) ou último bloco (IPv6) para reduzir granularidade.
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                parts[3] = "0";
                return String.join(".", parts);
            }
        }
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            parts[parts.length - 1] = "0000";
            return String.join(":", parts);
        }
        return "unknown";
    }
}
