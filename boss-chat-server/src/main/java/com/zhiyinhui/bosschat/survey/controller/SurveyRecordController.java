package com.zhiyinhui.bosschat.survey.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.course.dto.CourseCheckInRequest;
import com.zhiyinhui.bosschat.course.dto.CourseCheckInResponse;
import com.zhiyinhui.bosschat.course.dto.CoursePublicPhaseResponse;
import com.zhiyinhui.bosschat.survey.dto.SurveyListItemResponse;
import com.zhiyinhui.bosschat.survey.dto.SurveyRecordResponse;
import com.zhiyinhui.bosschat.survey.dto.SurveySubmitRequest;
import com.zhiyinhui.bosschat.survey.dto.SurveySubmitResponse;
import com.zhiyinhui.bosschat.survey.service.SurveyRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "企业需求调查")
@RestController
public class SurveyRecordController {

    private final SurveyRecordService surveyRecordService;

    public SurveyRecordController(SurveyRecordService surveyRecordService) {
        this.surveyRecordService = surveyRecordService;
    }

    @Operation(summary = "提交企业需求诊断问卷", description = "公开接口，客户填写后提交，服务端保存记录并调用两位 Kimi 智能体生成诊断结果。")
    @PostMapping("/api/public/surveys/enterprise-diagnosis")
    public SurveySubmitResponse submit(@Valid @RequestBody SurveySubmitRequest request) {
        return surveyRecordService.submit(request);
    }

    @Operation(summary = "查询公开课程期数信息", description = "公开问卷页根据二维码中的期数编码读取课程信息。")
    @GetMapping("/api/public/course-phases/{phaseCode}")
    public CoursePublicPhaseResponse publicPhase(@PathVariable String phaseCode) {
        return surveyRecordService.publicPhase(phaseCode);
    }

    @Operation(summary = "公开问卷学员签到校验", description = "学员填写姓名、手机号和身份证号，匹配本期学员名单后才能继续填写问卷。")
    @PostMapping("/api/public/course-phases/{phaseCode}/check-in")
    public CourseCheckInResponse checkIn(@PathVariable String phaseCode, @Valid @RequestBody CourseCheckInRequest request) {
        return surveyRecordService.checkIn(phaseCode, request);
    }

    @Operation(summary = "查询公开调查结果", description = "公开接口，用于客户提交后查看自己的 AI 诊断结果。")
    @GetMapping("/api/public/surveys/{publicId}")
    public SurveyRecordResponse publicDetail(@PathVariable String publicId) {
        return surveyRecordService.detail(publicId);
    }

    @Operation(summary = "流式生成公开调查结果", description = "公开 SSE 接口，用于客户结果页实时展示 AI 诊断报告生成过程。")
    @GetMapping(value = "/api/public/surveys/{publicId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPublicResult(@PathVariable String publicId) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                surveyRecordService.streamDiagnosis(
                        publicId,
                        stage -> sendEvent(emitter, "stage", stage),
                        delta -> sendEvent(emitter, "delta", delta)
                );
                sendEvent(emitter, "done", "诊断报告生成完成");
                emitter.complete();
            } catch (Exception exception) {
                sendEvent(emitter, "error", exception.getMessage() == null ? "AI 诊断生成失败" : exception.getMessage());
                emitter.complete();
            }
        });
        return emitter;
    }

    @Operation(summary = "查询调查记录列表", description = "后台列表，展示客户提交的企业 AI 需求诊断记录。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/api/survey-records")
    public List<SurveyListItemResponse> list(@RequestParam(required = false) Long phaseId) {
        StpUtil.checkLogin();
        return surveyRecordService.list(phaseId);
    }

    @Operation(summary = "查询调查记录详情", description = "后台详情，展示问卷原始答案、AI 中间分析和最终诊断结果。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/api/survey-records/{publicId}")
    public SurveyRecordResponse detail(@PathVariable String publicId) {
        StpUtil.checkLogin();
        return surveyRecordService.detail(publicId);
    }

    @Operation(summary = "鍒犻櫎璋冩煡璁板綍")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/api/survey-records/{publicId}")
    public void delete(@PathVariable String publicId) {
        StpUtil.checkLogin();
        surveyRecordService.delete(publicId);
    }

    @Operation(summary = "鎵归噺鍒犻櫎璋冩煡璁板綍")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/api/survey-records")
    public void deleteAll(@RequestParam(required = false) Long phaseId) {
        StpUtil.checkLogin();
        surveyRecordService.deleteAll(phaseId);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ignored) {
            emitter.complete();
        }
    }
}
