package org.example.smartlearning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.request.CreateNoteRequest;
import org.example.smartlearning.dto.response.ApiResponse;
import org.example.smartlearning.service.note.NoteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 笔记控制器
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ApiResponse<Void> createNote(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateNoteRequest request) {
        noteService.createNote(
                userId, request.getTitle(), request.getContent(),
                request.getSubjectId(), request.getKnowledgeId(), request.getIsPublic()
        );
        return ApiResponse.success("创建成功", null);
    }

    @PutMapping("/{noteId}")
    public ApiResponse<Void> updateNote(
            @AuthenticationPrincipal Long userId,
            @PathVariable("noteId") Long noteId,
            @RequestBody CreateNoteRequest request) {
        noteService.updateNote(userId, noteId, request.getTitle(), request.getContent(), request.getIsPublic());
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{noteId}")
    public ApiResponse<Void> deleteNote(
            @AuthenticationPrincipal Long userId,
            @PathVariable("noteId") Long noteId) {
        noteService.deleteNote(userId, noteId);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> getMyNotes(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(noteService.getUserNotes(userId));
    }

    @GetMapping("/public")
    public ApiResponse<Page<Map<String, Object>>> getPublicNotes(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(noteService.getPublicNotes(subjectId, page, size, userId));
    }

    @GetMapping("/{noteId}")
    public ApiResponse<Map<String, Object>> getNoteDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable("noteId") Long noteId) {
        return ApiResponse.success(noteService.getNoteDetail(noteId, userId));
    }

    @PostMapping("/{noteId}/like")
    public ApiResponse<Map<String, Object>> toggleLikeNote(
            @AuthenticationPrincipal Long userId,
            @PathVariable("noteId") Long noteId) {
        boolean liked = noteService.toggleLike(userId, noteId);
        Map<String, Object> result = Map.of(
            "liked", liked,
            "message", liked ? "点赞成功" : "取消点赞"
        );
        return ApiResponse.success(result);
    }

    @PostMapping("/{noteId}/collect")
    public ApiResponse<Void> collectNote(@PathVariable("noteId") Long noteId) {
        noteService.collectNote(noteId);
        return ApiResponse.success();
    }

    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> searchNotes(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return ApiResponse.success(noteService.searchNotes(keyword, limit));
    }
}
