package org.example.smartlearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.smartlearning.entity.PracticeRecord;

import java.util.List;
import java.util.Map;

/**
 * 专项练习记录Mapper
 */
@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {

    /**
     * 获取用户练习统计
     */
    @Select("SELECT practice_type, COUNT(*) as count, AVG(accuracy_rate) as avg_accuracy " +
            "FROM practice_record WHERE user_id = #{userId} AND status = 1 " +
            "GROUP BY practice_type")
    List<Map<String, Object>> getPracticeStatsByType(@Param("userId") Long userId);

    /**
     * 获取用户最近练习记录
     */
    @Select("SELECT pr.*, s.name as subject_name, kp.name as knowledge_name " +
            "FROM practice_record pr " +
            "LEFT JOIN subject s ON pr.subject_id = s.id " +
            "LEFT JOIN knowledge_point kp ON pr.knowledge_id = kp.id " +
            "WHERE pr.user_id = #{userId} AND pr.status = 1 " +
            "ORDER BY pr.end_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getRecentPractices(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 获取用户某科目练习统计
     */
    @Select("SELECT COUNT(*) as total, SUM(correct_count) as total_correct, " +
            "AVG(accuracy_rate) as avg_accuracy, SUM(duration) as total_duration " +
            "FROM practice_record WHERE user_id = #{userId} AND subject_id = #{subjectId} AND status = 1")
    Map<String, Object> getSubjectPracticeStats(@Param("userId") Long userId, @Param("subjectId") Long subjectId);
}