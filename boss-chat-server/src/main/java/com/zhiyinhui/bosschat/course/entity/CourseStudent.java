package com.zhiyinhui.bosschat.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("course_student")
public class CourseStudent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long phaseId;
    private Long groupId;
    private String studentNo;
    private String studentName;
    private String phone;
    private String idCard;
    private Integer isNewStudent;
    private Integer checkInCount;
    private LocalDateTime lastCheckInTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPhaseId() { return phaseId; }
    public void setPhaseId(Long phaseId) { this.phaseId = phaseId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public Integer getIsNewStudent() { return isNewStudent; }
    public void setIsNewStudent(Integer isNewStudent) { this.isNewStudent = isNewStudent; }
    public Integer getCheckInCount() { return checkInCount; }
    public void setCheckInCount(Integer checkInCount) { this.checkInCount = checkInCount; }
    public LocalDateTime getLastCheckInTime() { return lastCheckInTime; }
    public void setLastCheckInTime(LocalDateTime lastCheckInTime) { this.lastCheckInTime = lastCheckInTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
