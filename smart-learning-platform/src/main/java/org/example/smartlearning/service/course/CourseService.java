package org.example.smartlearning.service.course;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.entity.Course;
import org.example.smartlearning.entity.LikeRecord;
import org.example.smartlearning.entity.ViewRecord;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.CourseMapper;
import org.example.smartlearning.mapper.LikeRecordMapper;
import org.example.smartlearning.mapper.ViewRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程服务类
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final ViewRecordMapper viewRecordMapper;

    /**
     * 创建课程
     */
    @Transactional
    public Course createCourse(String title, String description, Long subjectId,
                                String videoUrl, Integer duration, Long teacherId, String teacherName) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setSubjectId(subjectId);
        course.setVideoUrl(videoUrl);
        course.setDuration(duration);
        course.setTeacherId(teacherId);
        course.setTeacherName(teacherName);
        course.setViewCount(0);
        course.setLikeCount(0);
        course.setDifficulty(3);
        course.setStatus(1);

        courseMapper.insert(course);
        return course;
    }

    /**
     * 获取课程列表
     */
    public Page<Map<String, Object>> getCourseList(Long subjectId, String keyword,
                                                    Integer difficulty, Integer page, Integer size, Long userId) {
        Page<Course> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 1)
                .orderByDesc(Course::getViewCount);

        if (subjectId != null) {
            wrapper.eq(Course::getSubjectId, subjectId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Course::getTitle, keyword);
        }

        if (difficulty != null) {
            wrapper.eq(Course::getDifficulty, difficulty);
        }

        Page<Course> coursePage = courseMapper.selectPage(pageParam, wrapper);

        Page<Map<String, Object>> resultPage = new Page<>(page, size, coursePage.getTotal());
        resultPage.setRecords(coursePage.getRecords().stream()
                .map(course -> convertToMap(course, userId))
                .collect(Collectors.toList()));

        return resultPage;
    }

    /**
     * 获取课程详情
     */
    @Transactional
    public Map<String, Object> getCourseDetail(Long courseId, Long userId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.of("课程不存在");
        }

        // 检查是否已浏览过，未浏览则增加浏览量
        if (userId != null) {
            LambdaQueryWrapper<ViewRecord> viewWrapper = new LambdaQueryWrapper<ViewRecord>()
                    .eq(ViewRecord::getUserId, userId)
                    .eq(ViewRecord::getTargetType, "COURSE")
                    .eq(ViewRecord::getTargetId, courseId);

            ViewRecord existingView = viewRecordMapper.selectOne(viewWrapper);
            if (existingView == null) {
                course.setViewCount(course.getViewCount() + 1);
                courseMapper.updateById(course);

                ViewRecord viewRecord = new ViewRecord();
                viewRecord.setUserId(userId);
                viewRecord.setTargetType("COURSE");
                viewRecord.setTargetId(courseId);
                viewRecordMapper.insert(viewRecord);
            }
        } else {
            course.setViewCount(course.getViewCount() + 1);
            courseMapper.updateById(course);
        }

        Map<String, Object> map = convertToMap(course, true, userId);
        return map;
    }

    /**
     * 点赞/取消点赞课程
     * @return true表示点赞成功，false表示取消点赞
     */
    @Transactional
    public boolean toggleLike(Long userId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.of("课程不存在");
        }

        // 检查是否已点赞
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, "COURSE")
                .eq(LikeRecord::getTargetId, courseId);

        LikeRecord existingLike = likeRecordMapper.selectOne(wrapper);

        if (existingLike != null) {
            // 已点赞，取消点赞
            likeRecordMapper.deleteById(existingLike.getId());
            course.setLikeCount(Math.max(0, course.getLikeCount() - 1));
            courseMapper.updateById(course);
            return false;
        } else {
            // 未点赞，添加点赞
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setUserId(userId);
            likeRecord.setTargetType("COURSE");
            likeRecord.setTargetId(courseId);
            likeRecordMapper.insert(likeRecord);

            course.setLikeCount(course.getLikeCount() + 1);
            courseMapper.updateById(course);
            return true;
        }
    }

    /**
     * 检查是否已点赞
     */
    public boolean isLiked(Long userId, Long courseId) {
        if (userId == null) return false;
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, "COURSE")
                .eq(LikeRecord::getTargetId, courseId);
        return likeRecordMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取热门课程
     */
    public List<Map<String, Object>> getHotCourses(Integer limit, Long userId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, 1)
                        .orderByDesc(Course::getViewCount)
                        .last("LIMIT " + limit)
        );

        return courses.stream()
                .map(course -> convertToMap(course, userId))
                .collect(Collectors.toList());
    }

    /**
     * 获取最新课程
     */
    public List<Map<String, Object>> getLatestCourses(Integer limit, Long userId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, 1)
                        .orderByDesc(Course::getCreatedAt)
                        .last("LIMIT " + limit)
        );

        return courses.stream()
                .map(course -> convertToMap(course, userId))
                .collect(Collectors.toList());
    }

    /**
     * 更新课程
     */
    @Transactional
    public void updateCourse(Long courseId, String title, String description,
                             String videoUrl, Integer difficulty) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.of("课程不存在");
        }

        if (title != null) course.setTitle(title);
        if (description != null) course.setDescription(description);
        if (videoUrl != null) course.setVideoUrl(videoUrl);
        if (difficulty != null) course.setDifficulty(difficulty);

        courseMapper.updateById(course);
    }

    /**
     * 删除课程
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            course.setStatus(0);
            courseMapper.updateById(course);
        }
    }

    /**
     * 转换课程为Map
     */
    private Map<String, Object> convertToMap(Course course, Long userId) {
        return convertToMap(course, false, userId);
    }

    private Map<String, Object> convertToMap(Course course, boolean includeDetails, Long userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", course.getId());
        map.put("title", course.getTitle());
        map.put("subjectId", course.getSubjectId());
        map.put("coverImage", course.getCoverImage());
        map.put("viewCount", course.getViewCount());
        map.put("likeCount", course.getLikeCount());
        map.put("difficulty", course.getDifficulty());
        map.put("teacherName", course.getTeacherName());
        map.put("createdAt", course.getCreatedAt());

        // 添加点赞状态
        if (userId != null) {
            map.put("liked", isLiked(userId, course.getId()));
        } else {
            map.put("liked", false);
        }

        if (includeDetails) {
            map.put("description", course.getDescription());
            map.put("videoUrl", course.getVideoUrl());
            map.put("duration", course.getDuration());
        } else {
            // 简短描述
            if (course.getDescription() != null && course.getDescription().length() > 100) {
                map.put("description", course.getDescription().substring(0, 100) + "...");
            } else {
                map.put("description", course.getDescription());
            }
        }

        return map;
    }
}
