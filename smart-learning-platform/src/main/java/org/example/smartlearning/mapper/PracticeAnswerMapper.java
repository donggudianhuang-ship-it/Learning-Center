package org.example.smartlearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.smartlearning.entity.PracticeAnswer;

import java.util.List;
import java.util.Map;

/**
 * 专项练习答题记录Mapper
 */
@Mapper
public interface PracticeAnswerMapper extends BaseMapper<PracticeAnswer> {

    /**
     * 获取练习记录的答题详情
     */
    @Select("SELECT pa.*, q.content as question_content, q.answer as correct_answer, q.analysis " +
            "FROM practice_answer pa " +
            "JOIN question q ON pa.question_id = q.id " +
            "WHERE pa.practice_record_id = #{practiceRecordId} " +
            "ORDER BY pa.question_order")
    List<Map<String, Object>> getAnswersByPracticeId(@Param("practiceRecordId") Long practiceRecordId);

    /**
     * 获取用户某题型的答题统计
     */
    @Select("SELECT q.type, COUNT(*) as total, SUM(pa.is_correct) as correct " +
            "FROM practice_answer pa " +
            "JOIN question q ON pa.question_id = q.id " +
            "WHERE pa.user_id = #{userId} " +
            "GROUP BY q.type")
    List<Map<String, Object>> getTypeStats(@Param("userId") Long userId);
}