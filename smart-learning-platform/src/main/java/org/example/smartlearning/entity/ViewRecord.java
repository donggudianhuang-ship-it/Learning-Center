package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 浏览记录实体类
 */
@Data
@TableName("view_record")
public class ViewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String targetType;  // POST, NOTE, COURSE

    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
