package org.example.smartlearning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏记录实体类（对应 collect_record 表）
 */
@Data
@TableName("collect_record")
public class CollectRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String targetType;  // POST, NOTE, COURSE, QUESTION

    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
