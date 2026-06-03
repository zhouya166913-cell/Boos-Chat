package com.zhiyinhui.bosschat.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.course.dto.CourseDashboardResponse;
import com.zhiyinhui.bosschat.course.dto.CoursePhaseRequest;
import com.zhiyinhui.bosschat.course.dto.CoursePhaseResponse;
import com.zhiyinhui.bosschat.course.dto.CourseStudentRequest;
import com.zhiyinhui.bosschat.course.dto.CourseStudentResponse;
import com.zhiyinhui.bosschat.course.entity.CoursePhase;
import com.zhiyinhui.bosschat.course.entity.CourseStudent;
import com.zhiyinhui.bosschat.course.mapper.CoursePhaseMapper;
import com.zhiyinhui.bosschat.course.mapper.CourseStudentMapper;
import com.zhiyinhui.bosschat.survey.entity.SurveyRecord;
import com.zhiyinhui.bosschat.survey.mapper.SurveyRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final CourseStudentMapper courseStudentMapper;
    private final SurveyRecordMapper surveyRecordMapper;
    private final ObjectMapper objectMapper;

    public CoursePhaseService(
            CoursePhaseMapper coursePhaseMapper,
            CourseStudentMapper courseStudentMapper,
            SurveyRecordMapper surveyRecordMapper,
            ObjectMapper objectMapper
    ) {
        this.coursePhaseMapper = coursePhaseMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.surveyRecordMapper = surveyRecordMapper;
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

    public List<CourseStudentResponse> listStudents(Long phaseId) {
        requirePhase(phaseId);
        return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getPhaseId, phaseId)
                        .orderByAsc(CourseStudent::getCreateTime))
                .stream()
                .map(this::toStudentResponse)
                .toList();
    }

    public CourseStudentResponse createStudent(Long phaseId, CourseStudentRequest request) {
        requirePhase(phaseId);
        ensurePhoneAvailable(phaseId, clean(request.phone()), null);
        CourseStudent student = new CourseStudent();
        student.setPhaseId(phaseId);
        fillStudent(student, request);
        courseStudentMapper.insert(student);
        bindExistingSurveyRecords(student);
        return toStudentResponse(student);
    }

    public CourseStudentResponse updateStudent(Long phaseId, Long studentId, CourseStudentRequest request) {
        requirePhase(phaseId);
        CourseStudent student = requireStudent(phaseId, studentId);
        ensurePhoneAvailable(phaseId, clean(request.phone()), studentId);
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
        List<SurveyRecord> records = surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                .eq(SurveyRecord::getPhaseId, phaseId)
                .orderByAsc(SurveyRecord::getCreateTime));

        Map<String, Long> painCounts = new LinkedHashMap<>();
        List<CourseDashboardResponse.StudentPainSummary> summaries = new ArrayList<>();
        Map<Long, CourseStudent> studentsById = new LinkedHashMap<>();
        Map<String, CourseStudent> studentsByPhone = new LinkedHashMap<>();
        for (CourseStudent student : students) {
            studentsById.put(student.getId(), student);
            studentsByPhone.put(student.getPhone(), student);
        }

        for (SurveyRecord record : records) {
            CourseStudent student = record.getStudentId() == null ? studentsByPhone.get(record.getPhone()) : studentsById.get(record.getStudentId());
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
        long submittedPhones = records.stream().map(SurveyRecord::getPhone).filter(phone -> !clean(phone).isBlank()).distinct().count();
        long missingSurvey = Math.max(0, students.size() - students.stream()
                .filter(student -> records.stream().anyMatch(record -> student.getPhone().equals(record.getPhone())))
                .count());

        return new CourseDashboardResponse(
                toPhaseResponse(phase),
                students.size(),
                newStudents,
                students.size() - newStudents,
                submittedPhones,
                completed,
                failed,
                missingSurvey,
                painStats,
                summaries,
                buildTeachingIdeas(painStats)
        );
    }

    public CoursePhase requirePhase(Long phaseId) {
        CoursePhase phase = coursePhaseMapper.selectById(phaseId);
        if (phase == null) {
            throw new ResponseStatusException(NOT_FOUND, "课程期数不存在");
        }
        return phase;
    }

    private void fillStudent(CourseStudent student, CourseStudentRequest request) {
        student.setStudentName(clean(request.studentName()));
        student.setPhone(clean(request.phone()));
        student.setIdCard(clean(request.idCard()));
        student.setIsNewStudent(enabledValue(request.isNewStudent()));
        student.setRemark(clean(request.remark()));
    }

    private void bindExistingSurveyRecords(CourseStudent student) {
        surveyRecordMapper.selectList(new LambdaQueryWrapper<SurveyRecord>()
                        .eq(SurveyRecord::getPhaseId, student.getPhaseId())
                        .eq(SurveyRecord::getPhone, student.getPhone()))
                .forEach(record -> {
                    record.setStudentId(student.getId());
                    surveyRecordMapper.updateById(record);
                });
    }

    private CoursePhaseResponse toPhaseResponse(CoursePhase phase) {
        long studentCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phase.getId()));
        long surveyRecordCount = surveyRecordMapper.selectCount(new LambdaQueryWrapper<SurveyRecord>()
                .eq(SurveyRecord::getPhaseId, phase.getId()));
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

    private CourseStudentResponse toStudentResponse(CourseStudent student) {
        return new CourseStudentResponse(
                student.getId(),
                student.getPhaseId(),
                student.getStudentName(),
                student.getPhone(),
                student.getIdCard(),
                student.getIsNewStudent(),
                student.getRemark(),
                student.getCreateTime(),
                student.getUpdateTime()
        );
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

    private void ensurePhoneAvailable(Long phaseId, String phone, Long ignoredStudentId) {
        CourseStudent student = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getPhaseId, phaseId)
                .eq(CourseStudent::getPhone, phone)
                .last("LIMIT 1"));
        if (student != null && !student.getId().equals(ignoredStudentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "该手机号已经在本期学员列表中");
        }
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
}
