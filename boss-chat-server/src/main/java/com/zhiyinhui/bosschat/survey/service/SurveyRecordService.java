package com.zhiyinhui.bosschat.survey.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.ai.dto.LlmChatResult;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiMessage;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import com.zhiyinhui.bosschat.ai.service.LlmChatService;
import com.zhiyinhui.bosschat.course.dto.CourseCheckInRequest;
import com.zhiyinhui.bosschat.course.dto.CourseCheckInResponse;
import com.zhiyinhui.bosschat.course.dto.CoursePublicPhaseResponse;
import com.zhiyinhui.bosschat.course.entity.CoursePhase;
import com.zhiyinhui.bosschat.course.entity.CourseStudent;
import com.zhiyinhui.bosschat.course.mapper.CoursePhaseMapper;
import com.zhiyinhui.bosschat.course.mapper.CourseStudentMapper;
import com.zhiyinhui.bosschat.survey.dto.SurveyListItemResponse;
import com.zhiyinhui.bosschat.survey.dto.SurveyRecordResponse;
import com.zhiyinhui.bosschat.survey.dto.SurveySubmitRequest;
import com.zhiyinhui.bosschat.survey.dto.SurveySubmitResponse;
import com.zhiyinhui.bosschat.survey.entity.SurveyRecord;
import com.zhiyinhui.bosschat.survey.mapper.SurveyRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SurveyRecordService {

    private static final String STATUS_ANALYZING = "ANALYZING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private final SurveyRecordMapper surveyRecordMapper;
    private final CoursePhaseMapper coursePhaseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final AiAgentMapper aiAgentMapper;
    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    public SurveyRecordService(
            SurveyRecordMapper surveyRecordMapper,
            CoursePhaseMapper coursePhaseMapper,
            CourseStudentMapper courseStudentMapper,
            AiAgentMapper aiAgentMapper,
            LlmChatService llmChatService,
            ObjectMapper objectMapper
    ) {
        this.surveyRecordMapper = surveyRecordMapper;
        this.coursePhaseMapper = coursePhaseMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.aiAgentMapper = aiAgentMapper;
        this.llmChatService = llmChatService;
        this.objectMapper = objectMapper;
    }

    public SurveySubmitResponse submit(SurveySubmitRequest request) {
        validate(request);
        CoursePhase phase = resolvePhase(request.phaseCode());
        CourseStudent student = phase == null ? null : requireMatchingStudent(
                phase,
                request.customerName()
        );
        SurveyRecord record = new SurveyRecord();
        record.setPhaseId(phase == null ? null : phase.getId());
        record.setStudentId(student == null ? null : student.getId());
        record.setPublicId(UUID.randomUUID().toString().replace("-", ""));
        record.setCustomerName(clean(request.customerName()));
        record.setPhone(clean(request.phone()));
        record.setIdCard(normalizeIdCard(request.idCard()));
        Integer isNewStudent = request.isNewStudent() == null && student != null
                ? student.getIsNewStudent()
                : studentTypeValue(request.isNewStudent());
        record.setIsNewStudent(isNewStudent);
        record.setCompany(clean(request.company()));
        record.setEmployeeCount(clean(request.employeeCount()));
        record.setAnnualRevenue(clean(request.annualRevenue()));
        record.setAnswersJson(toJson(buildAnswers(request)));
        record.setStatus(STATUS_ANALYZING);
        record.setErrorMessage("");
        surveyRecordMapper.insert(record);
        if (student != null) {
            syncStudentIdentity(student, request.phone(), request.idCard(), isNewStudent);
        }

        return new SurveySubmitResponse(record.getPublicId(), record.getStatus(), "");
    }

    public void streamDiagnosis(String publicId, Consumer<String> onStage, Consumer<String> onDelta) {
        SurveyRecord record = requireRecord(publicId);
        if (STATUS_COMPLETED.equals(record.getStatus()) && !clean(record.getFinalReport()).isBlank()) {
            onStage.accept("诊断报告已生成，正在展示结果");
            onDelta.accept(record.getFinalReport());
            return;
        }
        try {
            record.setStatus(STATUS_ANALYZING);
            record.setErrorMessage("");
            surveyRecordMapper.updateById(record);

            AiAgent analyzer = requireAgent("survey_demand_analyzer");
            AiAgent planner = requireAgent("survey_solution_planner");

            onStage.accept("第一位 AI 正在分析问卷和业务调研信息");
            String analyzerPrompt = buildAnalyzerPrompt(record);
            LlmChatResult analyzerResult = llmChatService.chat(analyzer, List.of(userMessage(analyzerPrompt)));

            onStage.accept("第二位 AI 正在生成落地诊断方案");
            String plannerPrompt = buildPlannerPrompt(record, analyzerResult.content());
            LlmChatResult plannerResult = llmChatService.stream(planner, List.of(userMessage(plannerPrompt)), onDelta);

            record.setAnalyzerResult(analyzerResult.content());
            record.setPlannerPrompt(plannerPrompt);
            record.setFinalReport(plannerResult.content());
            record.setStatus(STATUS_COMPLETED);
            record.setErrorMessage("");
            surveyRecordMapper.updateById(record);
        } catch (ResponseStatusException exception) {
            markFailed(record, exception.getReason());
            throw exception;
        } catch (Exception exception) {
            markFailed(record, exception.getMessage());
            throw exception;
        }
    }

    public List<SurveyListItemResponse> list(Long phaseId) {
        LambdaQueryWrapper<SurveyRecord> query = new LambdaQueryWrapper<SurveyRecord>()
                .orderByDesc(SurveyRecord::getCreateTime)
                .orderByDesc(SurveyRecord::getId);
        if (phaseId != null) {
            query.eq(SurveyRecord::getPhaseId, phaseId);
        }
        return latestVisibleRecords(surveyRecordMapper.selectList(query))
                .stream()
                .limit(200)
                .map(record -> new SurveyListItemResponse(
                        record.getPublicId(),
                        record.getPhaseId(),
                        phaseName(record.getPhaseId()),
                        record.getStudentId(),
                        recordNewFlag(record),
                        record.getCustomerName(),
                        record.getPhone(),
                        record.getIdCard(),
                        record.getCompany(),
                        record.getEmployeeCount(),
                        record.getAnnualRevenue(),
                        record.getStatus(),
                        record.getCreateTime()
                ))
                .toList();
    }

    private List<SurveyRecord> latestVisibleRecords(List<SurveyRecord> records) {
        Map<String, SurveyRecord> latestRecords = new LinkedHashMap<>();
        for (SurveyRecord record : records) {
            latestRecords.putIfAbsent(latestRecordKey(record), record);
        }
        return new ArrayList<>(latestRecords.values());
    }

    private String latestRecordKey(SurveyRecord record) {
        if (record.getStudentId() != null) {
            return "student:" + record.getStudentId();
        }
        String phasePrefix = record.getPhaseId() == null ? "phase:none" : "phase:" + record.getPhaseId();
        String name = clean(record.getCustomerName());
        if (!name.isBlank()) {
            return phasePrefix + ":name:" + name;
        }
        String phone = clean(record.getPhone());
        if (!phone.isBlank()) {
            return phasePrefix + ":phone:" + phone;
        }
        String publicId = clean(record.getPublicId());
        return publicId.isBlank() ? "record:" + record.getId() : "record:" + publicId;
    }

    public SurveyRecordResponse detail(String publicId) {
        return toResponse(requireRecord(publicId));
    }

    public void delete(String publicId) {
        surveyRecordMapper.deleteById(requireRecord(publicId).getId());
    }

    public void deleteAll(Long phaseId) {
        LambdaQueryWrapper<SurveyRecord> query = new LambdaQueryWrapper<>();
        if (phaseId != null) {
            if (coursePhaseMapper.selectById(phaseId) == null) {
                throw new ResponseStatusException(NOT_FOUND, "课程期数不存在");
            }
            query.eq(SurveyRecord::getPhaseId, phaseId);
        }
        surveyRecordMapper.delete(query);
    }

    public CoursePublicPhaseResponse publicPhase(String phaseCode) {
        CoursePhase phase = requireEnabledPhaseByCode(phaseCode);
        return new CoursePublicPhaseResponse(
                phase.getPhaseCode(),
                phase.getPhaseName(),
                phase.getCourseName(),
                phase.getSurveyPath(),
                phase.getRemark()
        );
    }

    public CourseCheckInResponse checkIn(String phaseCode, CourseCheckInRequest request) {
        CoursePhase phase = requireEnabledPhaseByCode(phaseCode);
        CourseStudent student = requireMatchingStudent(
                phase,
                request.studentName()
        );
        Integer isNewStudent = studentTypeValue(request.isNewStudent());
        syncStudentIdentity(student, request.phone(), request.idCard(), isNewStudent);
        markStudentCheckedIn(student);
        return new CourseCheckInResponse(
                phase.getId(),
                phase.getPhaseCode(),
                phase.getPhaseName(),
                student.getId(),
                student.getStudentName(),
                student.getPhone(),
                student.getIdCard(),
                student.getIsNewStudent(),
                safeCount(student.getCheckInCount()),
                student.getLastCheckInTime()
        );
    }

    private void markStudentCheckedIn(CourseStudent student) {
        student.setCheckInCount(safeCount(student.getCheckInCount()) + 1);
        student.setLastCheckInTime(LocalDateTime.now());
        courseStudentMapper.updateById(student);
    }

    private void validate(SurveySubmitRequest request) {
        if (request.painPoints() != null && request.painPoints().size() > 3) {
            throw new ResponseStatusException(BAD_REQUEST, "最头疼的问题最多选择3项");
        }
    }

    private SurveyRecord requireRecord(String publicId) {
        SurveyRecord record = surveyRecordMapper.selectOne(new LambdaQueryWrapper<SurveyRecord>()
                .eq(SurveyRecord::getPublicId, publicId)
                .last("LIMIT 1"));
        if (record == null) {
            throw new ResponseStatusException(NOT_FOUND, "调查记录不存在");
        }
        return record;
    }

    private CoursePhase resolvePhase(String phaseCode) {
        String cleaned = clean(phaseCode);
        if (cleaned.isBlank()) {
            return null;
        }
        return requireEnabledPhaseByCode(cleaned);
    }

    private CoursePhase requireEnabledPhaseByCode(String phaseCode) {
        String cleaned = clean(phaseCode);
        if (cleaned.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "课程期数不能为空");
        }
        CoursePhase phase = coursePhaseMapper.selectOne(new LambdaQueryWrapper<CoursePhase>()
                .eq(CoursePhase::getPhaseCode, cleaned)
                .last("LIMIT 1"));
        if (phase == null || !Integer.valueOf(1).equals(phase.getEnabled())) {
            throw new ResponseStatusException(BAD_REQUEST, "课程期数不存在或已停用");
        }
        return phase;
    }

    private CourseStudent requireMatchingStudent(CoursePhase phase, String studentName) {
        String cleanedName = clean(studentName);
        if (cleanedName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "请先填写姓名完成签到");
        }
        List<CourseStudent> candidates = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phase.getId())
                .eq(CourseStudent::getStudentName, cleanedName)
                .orderByAsc(CourseStudent::getCreateTime));
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "未找到本期学员，请确认姓名或联系老师");
        }
        return candidates.get(0);
    }

    private AiAgent requireAgent(String agentCode) {
        AiAgent agent = aiAgentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getAgentCode, agentCode)
                .eq(AiAgent::getEnabled, 1)
                .last("LIMIT 1"));
        if (agent == null) {
            throw new ResponseStatusException(BAD_REQUEST, "企业需求诊断智能体未配置");
        }
        return agent;
    }

    private AiMessage userMessage(String content) {
        AiMessage message = new AiMessage();
        message.setRole("user");
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    private Map<String, Object> buildAnswers(SurveySubmitRequest request) {
        Map<String, Object> answers = new LinkedHashMap<>();
        answers.put("公司主营产品/服务", clean(request.productService()));
        answers.put("主要客户群体", clean(request.targetCustomers()));
        answers.put("当前主要获客渠道", clean(request.acquisitionChannels()));
        answers.put("当前业务流程或系统使用情况", clean(request.workflowStatus()));
        answers.put("最头疼的3个问题", safeList(request.painPoints()));
        answers.put("问题对企业的最大影响", safeList(request.currentImpact()));
        answers.put("AI状态", clean(request.aiStatus()));
        answers.put("优先解决事项", clean(request.priorityGoal()));
        answers.put("推进AI的最大顾虑", clean(request.biggestObstacle()));
        answers.put("AI方案尝试意愿", clean(request.willingness()));
        answers.put("合作方式偏好", safeList(request.cooperationPreference()));
        answers.put("最希望优先解决的问题", clean(request.urgentProblem()));
        answers.put("是否愿意获得建议或后续交流", clean(request.followUpIntention()));
        return answers;
    }

    private String buildAnalyzerPrompt(SurveyRecord record) {
        return """
                请分析以下企业 AI 落地需求诊断表提交内容。

                ## 基础信息
                - 姓名：%s
                - 公司：%s
                - 电话：%s
                - 公司人数：%s
                - 公司业绩：%s

                ## 问卷与业务调研答案
                %s
                """.formatted(
                record.getCustomerName(),
                blank(record.getCompany()),
                blank(record.getPhone()),
                blank(record.getEmployeeCount()),
                blank(record.getAnnualRevenue()),
                prettyAnswers(record.getAnswersJson())
        );
    }

    private String buildPlannerPrompt(SurveyRecord record, String analyzerResult) {
        return """
                请基于第一阶段诊断分析，生成客户提交问卷后可以直接阅读的《企业 AI 落地诊断与建议》。
                这是一次性诊断报告，不是聊天对话。请直接给出结论、解决方案和落地路径，最后用“总结建议”自然收尾。
                必须重点结合“公司主营产品/服务、主要客户群体、获客渠道、业务流程或系统使用情况”做行业化判断，不要泛泛而谈。
                禁止在结尾向客户提问，禁止写“是否需要我继续”“如果你愿意我可以”等继续对话式话术。
                禁止使用 Markdown 表格，禁止输出 #、##、###、|、|:---|、---、--、[ ] 这类容易破坏页面排版的符号。
                标题请直接写中文标题，例如“诊断结论”“当前关键问题”“AI赋能切入点”“90天落地建议”“沟通与跟进建议”“总结建议”，不要在标题前加任何符号。
                内容要分段清楚：每个标题下面使用 2 到 5 条短句，让客户一眼看到重点。
                每个标题下面优先使用 1. 2. 3. 这样的数字序号组织内容，引导客户按顺序阅读；每条只表达一个重点。
                请你自己判断哪些内容是真正需要强调的重点，只允许用以下三种标记包裹重点文字：
                【痛点：客户最关键的经营痛点】
                【需求：客户最明确的业务需求】
                【关键方案：最值得优先执行的解决方案】
                每个小节最多标记 1 到 2 处重点，不要把普通标签、普通原因或整段话全部标记。

                ## 客户基础信息
                - 姓名：%s
                - 公司：%s
                - 电话：%s
                - 公司人数：%s
                - 公司业绩：%s

                ## 原始问卷与业务调研答案
                %s

                ## 第一阶段诊断分析
                %s
                """.formatted(
                record.getCustomerName(),
                blank(record.getCompany()),
                blank(record.getPhone()),
                blank(record.getEmployeeCount()),
                blank(record.getAnnualRevenue()),
                prettyAnswers(record.getAnswersJson()),
                analyzerResult
        );
    }

    private SurveyRecordResponse toResponse(SurveyRecord record) {
        return new SurveyRecordResponse(
                record.getPublicId(),
                record.getPhaseId(),
                phaseName(record.getPhaseId()),
                record.getStudentId(),
                recordNewFlag(record),
                record.getCustomerName(),
                record.getPhone(),
                record.getIdCard(),
                record.getCompany(),
                record.getEmployeeCount(),
                record.getAnnualRevenue(),
                parseAnswers(record.getAnswersJson()),
                record.getAnalyzerResult(),
                record.getPlannerPrompt(),
                record.getFinalReport(),
                record.getStatus(),
                record.getErrorMessage(),
                record.getCreateTime(),
                record.getUpdateTime()
        );
    }

    private String phaseName(Long phaseId) {
        if (phaseId == null) {
            return "";
        }
        CoursePhase phase = coursePhaseMapper.selectById(phaseId);
        return phase == null ? "" : phase.getPhaseName();
    }

    private Integer studentNewFlag(Long studentId) {
        if (studentId == null) {
            return null;
        }
        CourseStudent student = courseStudentMapper.selectById(studentId);
        return student == null ? null : student.getIsNewStudent();
    }

    private Integer recordNewFlag(SurveyRecord record) {
        return record.getIsNewStudent() != null ? record.getIsNewStudent() : studentNewFlag(record.getStudentId());
    }

    private void syncStudentIdentity(CourseStudent student, String phoneValue, String idCardValue, Integer isNewStudent) {
        String phone = clean(phoneValue);
        if (!phone.isBlank() && studentPhoneAvailable(student.getPhaseId(), phone, student.getId())) {
            student.setPhone(phone);
        }
        String idCard = normalizeIdCard(idCardValue);
        if (!idCard.isBlank()) {
            student.setIdCard(idCard);
        }
        if (isNewStudent != null) {
            student.setIsNewStudent(isNewStudent);
        }
        courseStudentMapper.updateById(student);
    }

    private boolean studentPhoneAvailable(Long phaseId, String phone, Long ignoredStudentId) {
        CourseStudent existing = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phaseId)
                .eq(CourseStudent::getPhone, phone)
                .last("LIMIT 1"));
        return existing == null || existing.getId().equals(ignoredStudentId);
    }

    private Integer safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private void markFailed(SurveyRecord record, String message) {
        record.setStatus(STATUS_FAILED);
        record.setErrorMessage(limit(clean(message), 1000));
        surveyRecordMapper.updateById(record);
    }

    private String prettyAnswers(String answersJson) {
        Map<String, Object> answers = parseAnswers(answersJson);
        StringBuilder builder = new StringBuilder();
        answers.forEach((key, value) -> builder.append("- ")
                .append(key)
                .append("：")
                .append(value instanceof List<?> list ? String.join("、", list.stream().map(String::valueOf).toList()) : blank(String.valueOf(value)))
                .append("\n"));
        return builder.toString();
    }

    private Map<String, Object> parseAnswers(String answersJson) {
        try {
            return objectMapper.readValue(answersJson, new TypeReference<>() {});
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "问卷答案格式错误");
        }
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::clean)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeIdCard(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private Integer studentTypeValue(Integer value) {
        return Integer.valueOf(0).equals(value) ? 0 : 1;
    }

    private String blank(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? "未填写" : cleaned;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
