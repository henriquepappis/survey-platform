package com.survey.service;

import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.entity.ResponseSession;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.SurveyRepository;
import com.survey.repository.VoteRepository;
import com.survey.repository.ResponseSessionRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SurveyExportService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final VoteRepository voteRepository;
    private final ResponseSessionRepository responseSessionRepository;

    public SurveyExportService(SurveyRepository surveyRepository,
                               QuestionRepository questionRepository,
                               OptionRepository optionRepository,
                               VoteRepository voteRepository,
                               ResponseSessionRepository responseSessionRepository) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
        this.responseSessionRepository = responseSessionRepository;
    }

    public byte[] exportSurveyAsXlsx(Long surveyId, boolean includeDeleted) {
        Survey survey = includeDeleted
                ? surveyRepository.findByIdIncludingDeleted(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + surveyId))
                : surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + surveyId));

        List<Question> questions = includeDeleted
                ? questionRepository.findBySurveyIdIncludingDeleted(surveyId)
                : questionRepository.findBySurveyIdOrderByOrdemAsc(surveyId);
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, List<Option>> optionsGrouped = questionIds.isEmpty() ? Map.of() :
                (includeDeleted
                        ? optionRepository.findByQuestionIdInIncludingDeleted(questionIds)
                        : optionRepository.findByQuestionIdIn(questionIds))
                        .stream()
                        .collect(Collectors.groupingBy(o -> o.getQuestion().getId()));

        List<VoteRepository.QuestionOptionCount> voteCounts = voteRepository.aggregateBySurvey(surveyId);
        List<ResponseSession> sessions = responseSessionRepository.findBySurveyId(surveyId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeOverviewSheet(workbook, survey, sessions, voteCounts);
            writeSurveySheet(workbook, survey);
            writeStructureSheet(workbook, questions, optionsGrouped);
            writeVotesSheet(workbook, voteCounts, survey.getTitulo());
            writeSessionsSheet(workbook, sessions);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao gerar XLSX", e);
        }
    }

    private void writeOverviewSheet(XSSFWorkbook workbook,
                                    Survey survey,
                                    List<ResponseSession> sessions,
                                    List<VoteRepository.QuestionOptionCount> voteCounts) {
        Sheet sheet = workbook.createSheet("Overview");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Métrica");
        header.createCell(1).setCellValue("Valor");

        int rowIdx = 1;
        rowIdx = writeMetric(sheet, rowIdx, "ID", String.valueOf(survey.getId()));
        rowIdx = writeMetric(sheet, rowIdx, "Título", nullSafe(survey.getTitulo()));
        rowIdx = writeMetric(sheet, rowIdx, "Descrição", nullSafe(survey.getDescricao()));
        rowIdx = writeMetric(sheet, rowIdx, "Ativo", Boolean.TRUE.equals(survey.getAtivo()) ? "sim" : "não");
        rowIdx = writeMetric(sheet, rowIdx, "Data validade", formatDateTime(survey.getDataValidade()));

        long totalResponses = sessions.size();
        long completed = sessions.stream().filter(s -> s.getStatus() == com.survey.entity.ResponseStatus.COMPLETED).count();
        long abandoned = sessions.stream().filter(s -> s.getStatus() == com.survey.entity.ResponseStatus.ABANDONED).count();
        double avgResponseTime = sessions.stream()
                .filter(s -> s.getStartedAt() != null && s.getCompletedAt() != null)
                .mapToDouble(s -> java.time.Duration.between(s.getStartedAt(), s.getCompletedAt()).toSeconds())
                .average()
                .orElse(0);

        rowIdx = writeMetric(sheet, rowIdx, "Respostas totais", String.valueOf(totalResponses));
        rowIdx = writeMetric(sheet, rowIdx, "Completas", String.valueOf(completed));
        rowIdx = writeMetric(sheet, rowIdx, "Abandonadas", String.valueOf(abandoned));
        rowIdx = writeMetric(sheet, rowIdx, "Taxa conclusão", totalResponses == 0 ? "0%" : String.format("%.2f%%", (double) completed / totalResponses * 100));
        rowIdx = writeMetric(sheet, rowIdx, "Taxa abandono", totalResponses == 0 ? "0%" : String.format("%.2f%%", (double) abandoned / totalResponses * 100));
        rowIdx = writeMetric(sheet, rowIdx, "Tempo médio (s)", String.format("%.2f", avgResponseTime));

        String predominantDevice = sessions.stream()
                .collect(Collectors.groupingBy(s -> nullSafe(s.getDeviceType()), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        rowIdx = writeMetric(sheet, rowIdx, "Dispositivo predominante", predominantDevice);

        String mostAbandonedQuestion = sessions.stream()
                .filter(s -> s.getStatus() == com.survey.entity.ResponseStatus.ABANDONED && s.getQuestion() != null)
                .collect(Collectors.groupingBy(s -> s.getQuestion().getTexto(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
        rowIdx = writeMetric(sheet, rowIdx, "Pergunta com mais abandono", mostAbandonedQuestion);

        long totalVotes = voteCounts.stream().mapToLong(VoteRepository.QuestionOptionCount::getTotal).sum();
        writeMetric(sheet, rowIdx, "Total de votos", String.valueOf(totalVotes));
    }

    private int writeMetric(Sheet sheet, int rowIdx, String name, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return rowIdx + 1;
    }

    private void writeSurveySheet(XSSFWorkbook workbook, Survey survey) {
        Sheet sheet = workbook.createSheet("Survey");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Título");
        header.createCell(2).setCellValue("Descrição");
        header.createCell(3).setCellValue("Ativo");
        header.createCell(4).setCellValue("Data Validade");
        header.createCell(5).setCellValue("Criado em");
        header.createCell(6).setCellValue("Atualizado em");
        header.createCell(7).setCellValue("Deletado em");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(survey.getId());
        row.createCell(1).setCellValue(nullSafe(survey.getTitulo()));
        row.createCell(2).setCellValue(nullSafe(survey.getDescricao()));
        row.createCell(3).setCellValue(Boolean.TRUE.equals(survey.getAtivo()) ? "sim" : "não");
        row.createCell(4).setCellValue(formatDateTime(survey.getDataValidade()));
        row.createCell(5).setCellValue(formatDateTime(survey.getCreatedAt()));
        row.createCell(6).setCellValue(formatDateTime(survey.getUpdatedAt()));
        row.createCell(7).setCellValue(formatDateTime(survey.getDeletedAt()));
    }

    private void writeStructureSheet(XSSFWorkbook workbook,
                                     List<Question> questions,
                                     Map<Long, List<Option>> optionsGrouped) {
        Sheet sheet = workbook.createSheet("Estrutura");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Pergunta ID");
        header.createCell(1).setCellValue("Pergunta");
        header.createCell(2).setCellValue("Ordem");
        header.createCell(3).setCellValue("Opção ID");
        header.createCell(4).setCellValue("Opção");
        header.createCell(5).setCellValue("Ativa");
        header.createCell(6).setCellValue("Deletada em");

        int rowIdx = 1;
        for (Question question : questions.stream()
                .sorted(Comparator.comparing(Question::getOrdem))
                .toList()) {
            List<Option> options = optionsGrouped.getOrDefault(question.getId(), List.of())
                    .stream()
                    .sorted(Comparator.comparing(Option::getId))
                    .toList();
            if (options.isEmpty()) {
                Row row = sheet.createRow(rowIdx++);
                fillStructureRow(row, question, null);
            } else {
                for (Option option : options) {
                    Row row = sheet.createRow(rowIdx++);
                    fillStructureRow(row, question, option);
                }
            }
        }
    }

    private void fillStructureRow(Row row, Question question, Option option) {
        row.createCell(0).setCellValue(question.getId());
        row.createCell(1).setCellValue(nullSafe(question.getTexto()));
        Cell ordemCell = row.createCell(2);
        if (question.getOrdem() != null) {
            ordemCell.setCellValue(question.getOrdem());
        }
        if (option != null) {
            row.createCell(3).setCellValue(option.getId());
            row.createCell(4).setCellValue(nullSafe(option.getTexto()));
            row.createCell(5).setCellValue(Boolean.TRUE.equals(option.getAtivo()) ? "sim" : "não");
            row.createCell(6).setCellValue(formatDateTime(option.getDeletedAt()));
        }
    }

    private void writeVotesSheet(XSSFWorkbook workbook, List<VoteRepository.QuestionOptionCount> counts, String surveyTitle) {
        XSSFSheet sheet = workbook.createSheet("Votos");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Pergunta ID");
        header.createCell(1).setCellValue("Pergunta");
        header.createCell(2).setCellValue("Opção ID");
        header.createCell(3).setCellValue("Opção");
        header.createCell(4).setCellValue("Total");
        header.createCell(5).setCellValue("Label");

        int rowIdx = 1;
        for (VoteRepository.QuestionOptionCount count : counts) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(count.getQuestionId());
            row.createCell(1).setCellValue(nullSafe(count.getQuestionText()));
            row.createCell(2).setCellValue(count.getOptionId());
            row.createCell(3).setCellValue(nullSafe(count.getOptionText()));
            row.createCell(4).setCellValue(count.getTotal());
            row.createCell(5).setCellValue("Q" + count.getQuestionId() + " - " + nullSafe(count.getOptionText()));
        }

        if (rowIdx > 1) {
            int chartTitleRowIdx = rowIdx + 1;
            Row titleRow = sheet.createRow(chartTitleRowIdx);
            titleRow.createCell(0).setCellValue("Votos por opção - " + nullSafe(surveyTitle));

            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            // Anchor for a more compact chart just below the title
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, chartTitleRowIdx + 1, 6, chartTitleRowIdx + 10);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("Votos por opção");
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.TOP_RIGHT);

            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFCategoryDataSource categories = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                    new CellRangeAddress(1, rowIdx - 1, 5, 5));
            XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(1, rowIdx - 1, 4, 4));

            XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
            data.setBarDirection(BarDirection.COL);
            XDDFChartData.Series series = data.addSeries(categories, values);
            series.setTitle("Votos", null);
            chart.plot(data);
        }
    }

    private void writeSessionsSheet(XSSFWorkbook workbook, List<ResponseSession> sessions) {
        Sheet sheet = workbook.createSheet("Sessions");
        Row header = sheet.createRow(0);
        String[] cols = {"Session ID", "Survey ID", "Question ID", "Status", "Device", "OS", "Browser", "Source",
                "Country", "State", "City", "StartedAt", "CompletedAt", "CreatedAt", "IP", "User-Agent"};
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
        int rowIdx = 1;
        for (ResponseSession s : sessions) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(s.getId());
            row.createCell(1).setCellValue(s.getSurvey() != null ? s.getSurvey().getId() : null);
            row.createCell(2).setCellValue(s.getQuestion() != null ? s.getQuestion().getId() : null);
            row.createCell(3).setCellValue(s.getStatus() != null ? s.getStatus().name() : "");
            row.createCell(4).setCellValue(nullSafe(s.getDeviceType()));
            row.createCell(5).setCellValue(nullSafe(s.getOperatingSystem()));
            row.createCell(6).setCellValue(nullSafe(s.getBrowser()));
            row.createCell(7).setCellValue(nullSafe(s.getSource()));
            row.createCell(8).setCellValue(nullSafe(s.getCountry()));
            row.createCell(9).setCellValue(nullSafe(s.getState()));
            row.createCell(10).setCellValue(nullSafe(s.getCity()));
            row.createCell(11).setCellValue(formatDateTime(s.getStartedAt()));
            row.createCell(12).setCellValue(formatDateTime(s.getCompletedAt()));
            row.createCell(13).setCellValue(formatDateTime(s.getCreatedAt()));
            row.createCell(14).setCellValue(nullSafe(s.getIpAddress()));
            row.createCell(15).setCellValue(nullSafe(s.getUserAgent()));
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
