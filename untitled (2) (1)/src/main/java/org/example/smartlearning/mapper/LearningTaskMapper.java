package org.example.smartlearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.smartlearning.entity.LearningTask;

@Mapper
public interface LearningTaskMapper extends BaseMapper<LearningTask> {
}
