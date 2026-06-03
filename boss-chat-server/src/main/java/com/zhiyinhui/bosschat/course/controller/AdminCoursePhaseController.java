package com.zhiyinhui.bosschat.course.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.service.AgentCancellationToken;
import com.zhiyinhui.bosschat.course.dto.CourseAnalysisRequest;
import com.zhiyinhui.bosschat.course.dto.CourseAnalysisResponse;
import com.zhiyinhui.bosschat.course.dto.CourseDashboardResponse;
import com.zhiyinhui.bosschat.course.dto.CoursePhaseRequest;
import com.zhiyinhui.bosschat.course.dto.CoursePhaseResponse;
import com.zhiyinhui.bosschat.course.dto.CourseStudentRequest;
import com.zhiyinhui.bosschat.course.dto.CourseStudentResponse;
import com.zhiyinhui.bosschat.course.service.CoursePhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Tag(name = "课程期数与签到")
@SecurityRequirement(name = "Sa-Token")
@RestController
@RequestMapping("/api/admin/course-phases")
public class AdminCoursePhaseController {

    private final CoursePhaseService coursePhaseService;

    public AdminCoursePhaseController(CoursePhaseService coursePhaseService) {
        this.coursePhaseService = coursePhaseService;
    }

    @Operation(summary = "查询课程期数列表")
    @GetMapping
    public List<CoursePhaseResponse> list() {
        StpUtil.checkLogin();
        return coursePhaseService.list();
    }

    @Operation(summary = "创建课程期数")
    @PostMapping
    public CoursePhaseResponse create(@Valid @RequestBody CoursePhaseRequest request) {
        StpUtil.checkLogin();
        return coursePhaseService.create(request);
    }

    @Operation(summary = "更新课程期数")
    @PutMapping("/{phaseId}")
    public CoursePhaseResponse update(@PathVariable Long phaseId, @Valid @RequestBody CoursePhaseRequest request) {
        StpUtil.checkLogin();
        return coursePhaseService.update(phaseId, request);
    }

    @Operation(summary = "查询期数学员列表")
    @GetMapping("/{phaseId}/students")
    public List<CourseStudentResponse> listStudents(@PathVariable Long phaseId) {
        StpUtil.checkLogin();
        return coursePhaseService.listStudents(phaseId);
    }

    @Operation(summary = "新增期数学员")
    @PostMapping("/{phaseId}/students")
    public CourseStudentResponse createStudent(@PathVariable Long phaseId, @Valid @RequestBody CourseStudentRequest request) {
        StpUtil.checkLogin();
        return coursePhaseService.createStudent(phaseId, request);
    }

    @Operation(summary = "更新期数学员")
    @PutMapping("/{phaseId}/students/{studentId}")
    public CourseStudentResponse updateStudent(
            @PathVariable Long phaseId,
            @PathVariable Long studentId,
            @Valid @RequestBody CourseStudentRequest request
    ) {
        StpUtil.checkLogin();
        return coursePhaseService.updateStudent(phaseId, studentId, request);
    }

    @Operation(summary = "删除期数学员")
    @DeleteMapping("/{phaseId}/students/{studentId}")
    public void deleteStudent(@PathVariable Long phaseId, @PathVariable Long studentId) {
        StpUtil.checkLogin();
        coursePhaseService.deleteStudent(phaseId, studentId);
    }

    @Operation(summary = "查询期数数据看板")
    @GetMapping("/{phaseId}/dashboard")
    public CourseDashboardResponse dashboard(@PathVariable Long phaseId) {
        StpUtil.checkLogin();
        return coursePhaseService.dashboard(phaseId);
    }

    @Operation(summary = "生成本期课程分析")
    @PostMapping("/{phaseId}/course-analysis")
    public CourseAnalysisResponse analyzeCourse(@PathVariable Long phaseId, @RequestBody(required = false) CourseAnalysisRequest request) {
        StpUtil.checkLogin();
        return coursePhaseService.analyzeCourse(phaseId, request);
    }

    @Operation(summary = "流式生成本期课程分析")
    @PostMapping(value = "/{phaseId}/course-analysis/stream", produces = "text/event-stream")
    public SseEmitter analyzeCourseStream(@PathVariable Long phaseId, @RequestBody(required = false) CourseAnalysisRequest request) {
        StpUtil.checkLogin();
        SseEmitter emitter = new SseEmitter(0L);
        AgentCancellationToken cancellationToken = new AgentCancellationToken();
        emitter.onCompletion(cancellationToken::cancel);
        emitter.onTimeout(cancellationToken::cancel);
        emitter.onError(error -> cancellationToken.cancel());

        CompletableFuture.runAsync(() -> {
            try {
                CourseAnalysisResponse response = coursePhaseService.analyzeCourse(
                        phaseId,
                        request,
                        delta -> sendEvent(emitter, "delta", Map.of("content", delta), cancellationToken)
                );
                sendEvent(emitter, "done", response, cancellationToken);
                emitter.complete();
            } catch (Exception exception) {
                sendEvent(emitter, "error", exception.getMessage() == null ? "课程分析失败" : exception.getMessage(), cancellationToken);
                emitter.complete();
            }
        });
        return emitter;
    }

    @GetMapping("/{phaseId}/course-analyses")
    public List<CourseAnalysisResponse> listCourseAnalyses(@PathVariable Long phaseId) {
        StpUtil.checkLogin();
        return coursePhaseService.listCourseAnalyses(phaseId);
    }

    private boolean sendEvent(SseEmitter emitter, String name, Object data, AgentCancellationToken cancellationToken) {
        if (cancellationToken.isCancelled()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
            return true;
        } catch (IOException exception) {
            cancellationToken.cancel();
            emitter.completeWithError(exception);
            return false;
        }
    }
}
