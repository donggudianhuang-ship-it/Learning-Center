package org.example.smartlearning.dto.request;

import lombok.Data;

/**
 * 试卷扫描判卷请求
 */
@Data
public class ScanGradeRequest {

    /**
     * 科目ID
     */
    private Long subjectId;

    /**
     * 年级
     */
    private String grade;

    /**
     * 备注
     */
    private String remark;
}
