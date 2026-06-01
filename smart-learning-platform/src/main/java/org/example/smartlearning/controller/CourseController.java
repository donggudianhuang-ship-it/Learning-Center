package org.example.smartlearning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.service.course.CourseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程控制器
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ApiResponse<Page<Map<String, Object>>> getCourseList(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "difficulty", required = false) Integer difficulty,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(courseService.getCourseList(subjectId, keyword, difficulty, page, size, userId));
    }

    @GetMapping("/hot")
    public ApiResponse<List<Map<String, Object>>> getHotCourses(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(courseService.getHotCourses(limit, userId));
    }

    @GetMapping("/latest")
    public ApiResponse<List<Map<String, Object>>> getLatestCourses(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(courseService.getLatestCourses(limit, userId));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<Map<String, Object>> getCourseDetail(
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(courseService.getCourseDetail(courseId, userId));
    }

    @PostMapping("/{courseId}/like")
    public ApiResponse<Map<String, Object>> toggleLikeCourse(
            @AuthenticationPrincipal Long userId,
            @PathVariable("courseId") Long courseId) {
        boolean liked = courseService.toggleLike(userId, courseId);
        Map<String, Object> result = Map.of(
            "liked", liked,
            "message", liked ? "点赞成功" : "取消点赞"
        );
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Void> createCourse(
            @AuthenticationPrincipal Long userId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam(value = "teacherName", required = false) String teacherName) {
        courseService.createCourse(title, description, subjectId, videoUrl, duration, userId, teacherName);
        return ApiResponse.success("创建成功", null);
    }

    @PutMapping("/{courseId}")
    public ApiResponse<Void> updateCourse(
            @PathVariable("courseId") Long courseId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam(value = "difficulty", required = false) Integer difficulty) {
        courseService.updateCourse(courseId, title, description, videoUrl, difficulty);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> deleteCourse(@PathVariable("courseId") Long courseId) {
        courseService.deleteCourse(courseId);
        return ApiResponse.success("删除成功", null);
    }
}
