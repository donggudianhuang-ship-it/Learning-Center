package org.example.smartlearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.smartlearning.entity.Question;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
