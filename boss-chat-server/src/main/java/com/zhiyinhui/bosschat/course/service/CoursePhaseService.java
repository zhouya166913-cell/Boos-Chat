package com.zhiyinhui.bosschat.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.ai.dto.LlmChatResult;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiMessage;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import com.zhiyinhui.bosschat.ai.service.LlmChatService;
import com.zhiyinhui.bosschat.course.dto.CourseAnalysisRequest;
import com.zhiyinhui.bosschat.course.dto.CourseAnalysisResponse;
import com.zhiyinhui.bosschat.course.dto.CourseDashboardResponse;
import com.zhiyinhui.bosschat.course.dto.CourseGroupRequest;
import com.zhiyinhui.bosschat.course.dto.CourseGroupResponse;
import com.zhiyinhui.bosschat.course.dto.CoursePhaseRequest;
import com.zhiyinhui.bosschat.course.dto.CoursePhaseResponse;
import com.zhiyinhui.bosschat.course.dto.CourseStudentRequest;
import com.zhiyinhui.bosschat.course.dto.CourseStudentResponse;
import com.zhiyinhui.bosschat.course.entity.CourseAnalysisHistory;
import com.zhiyinhui.bosschat.course.entity.CourseGroup;
import com.zhiyinhui.bosschat.course.entity.CoursePhase;
import com.zhiyinhui.bosschat.course.entity.CourseStudent;
import com.zhiyinhui.bosschat.course.mapper.CourseAnalysisHistoryMapper;
import com.zhiyinhui.bosschat.course.mapper.CourseGroupMapper;
import com.zhiyinhui.bosschat.course.mapper.CoursePhaseMapper;
import com.zhiyinhui.bosschat.course.mapper.CourseStudentMapper;
import com.zhiyinhui.bosschat.survey.entity.SurveyRecord;
import com.zhiyinhui.bosschat.survey.mapper.SurveyRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CoursePhaseService {

    private static final String DEFAULT_COURSE_NAME = "AI运营操盘手";
    private static final String SURVEY_PATH = "/survey/enterprise-diagnosis.html";
    private static final Pattern HIGHLIGHTED_PAIN_PATTERN = Pattern.compile("【痛点[：:](.+?)】");

    private final CoursePhaseMapper coursePhaseMapper;
    private final CourseGroupMapper courseGroupMapper;
    private final CourseAnalysisHistoryMapper courseAnalysisHistoryMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final AiAgentMapper aiAgentMapper;
    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    public CoursePhaseService(
            CoursePhaseMapper coursePhaseMapper,
            CourseGroupMapper courseGroupMapper,
            CourseAnalysisHistoryMapper courseAnalysisHistoryMapper,
            CourseStudentMapper courseStudentMapper,
            SurveyRecordMapper surveyRecordMapper,
            AiAgentMapper aiAgentMapper,
            LlmChatService llmChatService,
            ObjectMapper objectMapper
    ) {
        this.coursePhaseMapper = coursePhaseMapper;
        this.courseGroupMapper = courseGroupMapper;
        this.courseAnalysisHistoryMapper = courseAnalysisHistoryMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.surveyRecordMapper = surveyRecordMapper;
        this.aiAgentMapper = aiAgentMapper;
        this.llmChatService = llmChatService;
        this.objectMapper = objectMapper;
    }

    public List<CoursePhaseResponse> list() {
        return coursePhaseMapper.selectList(new LambdaQueryWrapper<CoursePhase>()
                        .orderByDesc(CoursePhase::getCreateTime))
                .stream()
                .map(this::toPhaseResponse)
                .toList();
    }

    public CoursePhaseResponse create(CoursePhaseRequest request) {
        CoursePhase phase = new CoursePhase();
        phase.setPhaseCode(generatePhaseCode());
        phase.setPhaseName(clean(request.phaseName()));
        phase.setCourseName(defaulted(request.courseName(), DEFAULT_COURSE_NAME));
        phase.setSurveyPath(SURVEY_PATH);
        phase.setQrImageUrl(clean(request.qrImageUrl()));
        phase.setEnabled(enabledValue(request.enabled()));
        phase.setRemark(clean(request.remark()));
        coursePhaseMapper.insert(phase);
        return toPhaseResponse(phase);
    }

    public CoursePhaseResponse update(Long phaseId, CoursePhaseRequest request) {
        CoursePhase phase = requirePhase(phaseId);
        phase.setPhaseName(clean(request.phaseName()));
        phase.setCourseName(defaulted(request.courseName(), DEFAULT_COURSE_NAME));
        phase.setQrImageUrl(clean(request.qrImageUrl()));
        phase.setEnabled(enabledValue(request.enabled()));
        phase.setRemark(clean(request.remark()));
        coursePhaseMapper.updateById(phase);
        return toPhaseResponse(requirePhase(phaseId));
    }

    public List<CourseGroupResponse> listGroups(Long phaseId) {
        requirePhase(phaseId);
        return courseGroupMapper.selectList(new LambdaQueryWrapper<CourseGroup>()
                        .eq(CourseGroup::getPhaseId, phaseId)
                        .orderByAsc(CourseGroup::getSortOrder)
                        .orderByAsc(CourseGroup::getId))
                .stream()
                .map(this::toGroupResponse)
                .toList();
    }

    public CourseGroupResponse createGroup(Long phaseId, CourseGroupRequest request) {
        requirePhase(phaseId);
        ensureGroupNameAvailable(phaseId, clean(request.groupName()), null);
        CourseGroup group = new CourseGroup();
        group.setPhaseId(phaseId);
        fillGroup(group, request);
        courseGroupMapper.insert(group);
        return toGroupResponse(group);
    }

    public CourseGroupResponse updateGroup(Long phaseId, Long groupId, CourseGroupRequest request) {
        requirePhase(phaseId);
        CourseGroup group = requireGroup(phaseId, groupId);
        ensureGroupNameAvailable(phaseId, clean(request.groupName()), groupId);
        fillGroup(group, request);
        courseGroupMapper.updateById(group);
        return toGroupResponse(requireGroup(phaseId, groupId));
    }

    public void deleteGroup(Long phaseId, Long groupId) {
        CourseGroup group = requireGroup(phaseId, groupId);
        long studentCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getGroupId, groupId));
        if (studentCount > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "请先删除或移动组内学员");
        }
        courseGroupMapper.deleteById(group.getId());
    }

    public List<CourseStudentResponse> listStudents(Long phaseId) {
        requirePhase(phaseId);
        return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getPhaseId, phaseId)
                        .orderByAsc(CourseStudent::getGroupId)
                        .orderByAsc(CourseStudent::getCreateTime))
                .stream()
                .map(this::toStudentResponse)
                .toList();
    }

    public CourseStudentResponse createStudent(Long phaseId, CourseStudentRequest request) {
        requirePhase(phaseId);
        ensurePhoneAvailable(phaseId, clean(request.phone()), null);
        ensureStudentNoAvailable(phaseId, clean(request.studentNo()), null);
        CourseStudent student = new CourseStudent();
        student.setPhaseId(phaseId);
        student.setCheckInCount(0);
        fillStudent(student, request);
        courseStudentMapper.insert(student);
        bindExistingSurveyRecords(student);
        return toStudentResponse(student);
    }

    public CourseStudentResponse updateStudent(Long phaseId, Long studentId, CourseStudentRequest request) {
        requirePhase(phaseId);
        CourseStudent student = requireStudent(phaseId, studentId);
        ensurePhoneAvailable(phaseId, clean(request.phone()), studentId);
        ensureStudentNoAvailable(phaseId, clean(request.studentNo()), studentId);
        fillStudent(student, request);
        courseStudentMapper.updateById(student);
        bindExistingSurveyRecords(student);
        return toStudentResponse(requireStudent(phaseId, studentId));
    }

    public void deleteStudent(Long phaseId, Long studentId) {
        requireStudent(phaseId, studentId);
        surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                        .eq(SurveyRecord::getStudentId, studentId))
                .forEach(record -> {
                    record.setStudentId(null);
                    surveyRecordMapper.updateById(record);
                });
        courseStudentMapper.deleteById(studentId);
    }

    public CourseDashboardResponse dashboard(Long phaseId) {
        CoursePhase phase = requirePhase(phaseId);
        List<CourseStudent> students = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phaseId)
                .orderByAsc(CourseStudent::getCreateTime));
        List<SurveyRecord> records = latestVisibleRecords(surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                .eq(SurveyRecord::getPhaseId, phaseId)
                .orderByDesc(SurveyRecord::getCreateTime)
                .orderByDesc(SurveyRecord::getId)));

        Map<String, Long> painCounts = new LinkedHashMap<>();
        List<CourseDashboardResponse.StudentPainSummary> summaries = new ArrayList<>();
        Map<Long, CourseStudent> studentsById = new LinkedHashMap<>();
        Map<String, CourseStudent> studentsByPhone = new LinkedHashMap<>();
        for (CourseStudent student : students) {
            studentsById.put(student.getId(), student);
            String phone = clean(student.getPhone());
            if (!phone.isBlank()) {
                studentsByPhone.put(phone, student);
            }
        }

        for (SurveyRecord record : records) {
            CourseStudent student = record.getStudentId() == null ? studentsByPhone.get(clean(record.getPhone())) : studentsById.get(record.getStudentId());
            List<String> pains = extractPainPoints(record);
            for (String pain : pains) {
                painCounts.merge(pain, 1L, Long::sum);
            }
            summaries.add(new CourseDashboardResponse.StudentPainSummary(
                    student == null ? record.getCustomerName() : student.getStudentName(),
                    record.getPhone(),
                    record.getStatus(),
                    pains,
                    summarizeReport(record.getFinalReport())
            ));
        }

        List<CourseDashboardResponse.PainPointStat> painStats = painCounts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new CourseDashboardResponse.PainPointStat(entry.getKey(), entry.getValue()))
                .toList();

        long completed = records.stream().filter(record -> "COMPLETED".equals(record.getStatus())).count();
        long failed = records.stream().filter(record -> "FAILED".equals(record.getStatus())).count();
        long newStudents = students.stream().filter(student -> Integer.valueOf(1).equals(student.getIsNewStudent())).count();
        long submittedStudents = records.stream()
                .map(this::submittedRecordKey)
                .filter(key -> !key.isBlank())
                .distinct()
                .count();
        long missingSurvey = Math.max(0, students.size() - students.stream()
                .filter(student -> records.stream().anyMatch(record -> recordMatchesStudent(record, student)))
                .count());

        return new CourseDashboardResponse(
                toPhaseResponse(phase),
                students.size(),
                newStudents,
                students.size() - newStudents,
                submittedStudents,
                completed,
                failed,
                missingSurvey,
                painStats,
                summaries,
                buildTeachingIdeas(painStats)
        );
    }

    public CourseAnalysisResponse analyzeCourse(Long phaseId, CourseAnalysisRequest request) {
        return analyzeCourse(phaseId, request, null);
    }

    public CourseAnalysisResponse analyzeCourse(Long phaseId, CourseAnalysisRequest request, Consumer<String> onDelta) {
        CoursePhase phase = requirePhase(phaseId);
        CourseDashboardResponse dashboard = dashboard(phaseId);
        boolean hasPainData = !dashboard.painPoints().isEmpty()
                || dashboard.studentPainSummaries().stream().anyMatch(summary -> !summary.painPoints().isEmpty());
        if (!hasPainData) {
            throw new ResponseStatusException(BAD_REQUEST, "本期暂无有效痛点样本，无法生成课程分析");
        }
        AiAgent agent = request != null && request.agentId() != null
                ? requireEnabledAgent(request.agentId())
                : requireAgent("survey_solution_planner");
        LlmChatResult result = onDelta == null
                ? llmChatService.chat(agent, List.of(userMessage(buildCourseAnalysisPrompt(phase, dashboard))))
                : llmChatService.stream(agent, List.of(userMessage(buildCourseAnalysisPrompt(phase, dashboard))), onDelta);
        CourseAnalysisHistory history = new CourseAnalysisHistory();
        history.setPhaseId(phase.getId());
        history.setAgentId(agent.getId());
        history.setAgentName(agent.getAgentName());
        history.setContent(result.content());
        history.setCreateTime(LocalDateTime.now());
        courseAnalysisHistoryMapper.insert(history);
        return toAnalysisResponse(history, phase);
    }

    public List<CourseAnalysisResponse> listCourseAnalyses(Long phaseId) {
        CoursePhase phase = requirePhase(phaseId);
        return courseAnalysisHistoryMapper.selectList(new LambdaQueryWrapper<CourseAnalysisHistory>()
                        .eq(CourseAnalysisHistory::getPhaseId, phaseId)
                        .orderByDesc(CourseAnalysisHistory::getCreateTime)
                        .last("LIMIT 30"))
                .stream()
                .map(history -> toAnalysisResponse(history, phase))
                .toList();
    }

    private CourseAnalysisResponse toAnalysisResponse(CourseAnalysisHistory history, CoursePhase phase) {
        return new CourseAnalysisResponse(
                history.getId(),
                phase.getId(),
                phase.getPhaseName(),
                history.getAgentId(),
                history.getAgentName(),
                history.getContent(),
                history.getCreateTime()
        );
    }

    public CoursePhase requirePhase(Long phaseId) {
        CoursePhase phase = coursePhaseMapper.selectById(phaseId);
        if (phase == null) {
            throw new ResponseStatusException(NOT_FOUND, "课程期数不存在");
        }
        return phase;
    }

    private AiAgent requireAgent(String agentCode) {
        AiAgent agent = aiAgentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getAgentCode, agentCode)
                .eq(AiAgent::getEnabled, 1)
                .last("LIMIT 1"));
        if (agent == null) {
            throw new ResponseStatusException(BAD_REQUEST, "课程分析智能体未配置");
        }
        return agent;
    }

    private AiAgent requireEnabledAgent(Long agentId) {
        AiAgent agent = aiAgentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getId, agentId)
                .eq(AiAgent::getEnabled, 1)
                .last("LIMIT 1"));
        if (agent == null) {
            throw new ResponseStatusException(BAD_REQUEST, "课程分析智能体未配置");
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

    private String buildCourseAnalysisPrompt(CoursePhase phase, CourseDashboardResponse dashboard) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                你是蓝图商学 AI 运营课程教研助理。请基于本期学员调查问卷中的痛点，整理给老师备课用的课程分析。
                输出要求：
                1. 不要编造未出现的学员痛点。
                2. 先按高频痛点排序，再给出课程主线。
                3. 给出课堂案例、现场演练、答疑优先级和课后跟进建议。
                4. 用清晰分段和编号输出，方便老师直接拿去整理授课内容。

                ## 课程期数
                """)
                .append(phase.getPhaseName())
                .append("\n\n## 样本概况\n")
                .append("- 学员数：").append(dashboard.totalStudents()).append("\n")
                .append("- 已提交问卷：").append(dashboard.submittedCount()).append("\n")
                .append("- 新学员：").append(dashboard.newStudentCount()).append("\n")
                .append("- 未提交问卷：").append(dashboard.missingSurveyCount()).append("\n\n")
                .append("## 高频痛点\n");
        for (CourseDashboardResponse.PainPointStat stat : dashboard.painPoints()) {
            builder.append("- ").append(stat.painPoint()).append("：").append(stat.count()).append("次\n");
        }
        builder.append("\n## 学员痛点明细\n");
        for (CourseDashboardResponse.StudentPainSummary summary : dashboard.studentPainSummaries()) {
            if (summary.painPoints().isEmpty()) {
                continue;
            }
            builder.append("- ")
                    .append(summary.studentName())
                    .append("（")
                    .append(summary.phone())
                    .append("）：")
                    .append(String.join("；", summary.painPoints()))
                    .append("\n");
        }
        return builder.toString();
    }

    private void fillStudent(CourseStudent student, CourseStudentRequest request) {
        Long groupId = request.groupId();
        if (groupId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "请选择分组");
        }
        requireGroup(student.getPhaseId(), groupId);
        student.setGroupId(groupId);
        student.setStudentNo(clean(request.studentNo()));
        student.setStudentName(clean(request.studentName()));
        student.setPhone(nullableClean(request.phone()));
        student.setIdCard(clean(request.idCard()));
        student.setIsNewStudent(enabledValue(request.isNewStudent()));
        student.setRemark(clean(request.remark()));
    }

    private void fillGroup(CourseGroup group, CourseGroupRequest request) {
        group.setGroupName(clean(request.groupName()));
        group.setLeaderName(clean(request.leaderName()));
        group.setRemark(clean(request.remark()));
        group.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void bindExistingSurveyRecords(CourseStudent student) {
        surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                        .eq(SurveyRecord::getPhaseId, student.getPhaseId())
                        .eq(SurveyRecord::getCustomerName, student.getStudentName()))
                .forEach(record -> {
                    record.setStudentId(student.getId());
                    if (record.getIsNewStudent() == null) {
                        record.setIsNewStudent(student.getIsNewStudent());
                    }
                    surveyRecordMapper.updateById(record);
                });
    }

    private List<SurveyRecord> latestVisibleRecords(List<SurveyRecord> records) {
        Map<String, SurveyRecord> latestRecords = new LinkedHashMap<>();
        for (SurveyRecord record : records) {
            latestRecords.putIfAbsent(latestRecordKey(record), record);
        }
        return new ArrayList<>(latestRecords.values());
    }

    private String latestRecordKey(SurveyRecord record) {
        String submittedKey = submittedRecordKey(record);
        if (!submittedKey.isBlank()) {
            return submittedKey;
        }
        String publicId = clean(record.getPublicId());
        return publicId.isBlank() ? "record:" + record.getId() : "record:" + publicId;
    }

    private CoursePhaseResponse toPhaseResponse(CoursePhase phase) {
        long studentCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phase.getId()));
        long surveyRecordCount = latestVisibleRecords(surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                .eq(SurveyRecord::getPhaseId, phase.getId())
                .orderByDesc(SurveyRecord::getCreateTime)
                .orderByDesc(SurveyRecord::getId))).size();
        return new CoursePhaseResponse(
                phase.getId(),
                phase.getPhaseCode(),
                phase.getPhaseName(),
                phase.getCourseName(),
                phase.getSurveyPath(),
                phase.getQrImageUrl(),
                phase.getEnabled(),
                phase.getRemark(),
                studentCount,
                surveyRecordCount,
                phase.getCreateTime(),
                phase.getUpdateTime()
        );
    }

    private CourseGroupResponse toGroupResponse(CourseGroup group) {
        long studentCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getGroupId, group.getId()));
        return new CourseGroupResponse(
                group.getId(),
                group.getPhaseId(),
                group.getGroupName(),
                group.getLeaderName(),
                group.getRemark(),
                group.getSortOrder(),
                studentCount,
                group.getCreateTime(),
                group.getUpdateTime()
        );
    }

    private CourseStudentResponse toStudentResponse(CourseStudent student) {
        CourseGroup group = student.getGroupId() == null ? null : courseGroupMapper.selectById(student.getGroupId());
        return new CourseStudentResponse(
                student.getId(),
                student.getPhaseId(),
                student.getGroupId(),
                group == null ? "" : group.getGroupName(),
                group == null ? "" : group.getLeaderName(),
                student.getStudentNo(),
                student.getStudentName(),
                student.getPhone(),
                student.getIdCard(),
                student.getIsNewStudent(),
                safeCount(student.getCheckInCount()),
                student.getLastCheckInTime(),
                student.getRemark(),
                student.getCreateTime(),
                student.getUpdateTime()
        );
    }

    private Integer safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private CourseStudent requireStudent(Long phaseId, Long studentId) {
        CourseStudent student = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getId, studentId)
                .eq(CourseStudent::getPhaseId, phaseId)
                .last("LIMIT 1"));
        if (student == null) {
            throw new ResponseStatusException(NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private CourseGroup requireGroup(Long phaseId, Long groupId) {
        CourseGroup group = courseGroupMapper.selectOne(new LambdaQueryWrapper<CourseGroup>()
                .eq(CourseGroup::getId, groupId)
                .eq(CourseGroup::getPhaseId, phaseId)
                .last("LIMIT 1"));
        if (group == null) {
            throw new ResponseStatusException(NOT_FOUND, "分组不存在");
        }
        return group;
    }

    private void ensureGroupNameAvailable(Long phaseId, String groupName, Long ignoredGroupId) {
        if (clean(groupName).isBlank()) {
            return;
        }
        CourseGroup group = courseGroupMapper.selectOne(new LambdaQueryWrapper<CourseGroup>()
                .eq(CourseGroup::getPhaseId, phaseId)
                .eq(CourseGroup::getGroupName, groupName)
                .last("LIMIT 1"));
        if (group != null && !group.getId().equals(ignoredGroupId)) {
            throw new ResponseStatusException(BAD_REQUEST, "本期已存在同名分组");
        }
    }

    private void ensurePhoneAvailable(Long phaseId, String phone, Long ignoredStudentId) {
        if (clean(phone).isBlank()) {
            return;
        }
        CourseStudent student = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phaseId)
                .eq(CourseStudent::getPhone, phone)
                .last("LIMIT 1"));
        if (student != null && !student.getId().equals(ignoredStudentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "该手机号已经在本期学员列表中");
        }
    }

    private void ensureStudentNoAvailable(Long phaseId, String studentNo, Long ignoredStudentId) {
        if (clean(studentNo).isBlank()) {
            return;
        }
        CourseStudent student = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phaseId)
                .eq(CourseStudent::getStudentNo, studentNo)
                .last("LIMIT 1"));
        if (student != null && !student.getId().equals(ignoredStudentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "该学号已经在本期学员列表中");
        }
    }

    private boolean recordMatchesStudent(SurveyRecord record, CourseStudent student) {
        if (record.getStudentId() != null) {
            return record.getStudentId().equals(student.getId());
        }
        String recordName = clean(record.getCustomerName());
        String studentName = clean(student.getStudentName());
        if (!recordName.isBlank() && recordName.equals(studentName)) {
            return true;
        }
        String recordPhone = clean(record.getPhone());
        String studentPhone = clean(student.getPhone());
        return !recordPhone.isBlank() && recordPhone.equals(studentPhone);
    }

    private String submittedRecordKey(SurveyRecord record) {
        if (record.getStudentId() != null) {
            return "student:" + record.getStudentId();
        }
        String name = clean(record.getCustomerName());
        if (!name.isBlank()) {
            return "name:" + name;
        }
        String phone = clean(record.getPhone());
        return phone.isBlank() ? "" : "phone:" + phone;
    }

    private List<String> extractPainPoints(SurveyRecord record) {
        LinkedHashSet<String> pains = new LinkedHashSet<>();
        Matcher matcher = HIGHLIGHTED_PAIN_PATTERN.matcher(clean(record.getFinalReport()));
        while (matcher.find()) {
            addPain(pains, matcher.group(1));
        }

        Map<String, Object> answers = parseAnswers(record.getAnswersJson());
        Object painAnswer = answers.get("最头疼的3个问题");
        if (painAnswer instanceof List<?> list) {
            list.forEach(value -> addPain(pains, String.valueOf(value)));
        } else {
            addPain(pains, String.valueOf(painAnswer));
        }
        Object urgent = answers.get("最希望优先解决的问题");
        addPain(pains, String.valueOf(urgent));
        return pains.stream().limit(8).toList();
    }

    private void addPain(LinkedHashSet<String> pains, String value) {
        String cleaned = clean(value)
                .replace("。", "")
                .replace("；", "")
                .replace(";", "");
        if (!cleaned.isBlank() && !"null".equalsIgnoreCase(cleaned)) {
            pains.add(cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned);
        }
    }

    private String summarizeReport(String finalReport) {
        String cleaned = clean(finalReport);
        if (cleaned.isBlank()) {
            return "";
        }
        return cleaned.length() > 240 ? cleaned.substring(0, 240) + "..." : cleaned;
    }

    private List<String> buildTeachingIdeas(List<CourseDashboardResponse.PainPointStat> painStats) {
        if (painStats.isEmpty()) {
            return List.of("本期暂未形成有效痛点样本，建议先引导学员完成调查问卷，再整理课程重点。");
        }
        List<String> ideas = new ArrayList<>();
        painStats.stream().limit(5).forEach(stat -> ideas.add("围绕“" + stat.painPoint() + "”安排案例拆解、工具演示和现场练习。"));
        ideas.add("把高频痛点按获客、成交、团队、流程、AI工具使用分组，先讲共性问题，再做个案答疑。");
        ideas.add("对未提交问卷的学员课前补采集，避免课堂内容只覆盖少数学员的诉求。");
        return ideas;
    }

    private Map<String, Object> parseAnswers(String answersJson) {
        try {
            return objectMapper.readValue(answersJson, new TypeReference<>() {});
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String generatePhaseCode() {
        return "phase_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private Integer enabledValue(Integer value) {
        return value == null || value != 0 ? 1 : 0;
    }

    private String defaulted(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullableClean(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }
}
