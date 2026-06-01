package org.example.smartlearning.service.note;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.entity.LikeRecord;
import org.example.smartlearning.entity.StudyNote;
import org.example.smartlearning.entity.User;
import org.example.smartlearning.entity.ViewRecord;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.LikeRecordMapper;
import org.example.smartlearning.mapper.StudyNoteMapper;
import org.example.smartlearning.mapper.UserMapper;
import org.example.smartlearning.mapper.ViewRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习笔记服务类
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final StudyNoteMapper noteMapper;
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final ViewRecordMapper viewRecordMapper;

    /**
     * 创建笔记
     */
    @Transactional
    public StudyNote createNote(Long userId, String title, String content, Long subjectId, Long knowledgeId, boolean isPublic) {
        StudyNote note = new StudyNote();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content);
        note.setSubjectId(subjectId);
        note.setKnowledgeId(knowledgeId);
        note.setIsPublic(isPublic ? 1 : 0);
        note.setViewCount(0);
        note.setLikeCount(0);
        note.setCollectCount(0);

        noteMapper.insert(note);
        return note;
    }

    /**
     * 更新笔记
     */
    @Transactional
    public void updateNote(Long userId, Long noteId, String title, String content, Boolean isPublic) {
        StudyNote note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of("笔记不存在");
        }

        if (!note.getUserId().equals(userId)) {
            throw BusinessException.of("无权修改此笔记");
        }

        if (title != null) {
            note.setTitle(title);
        }
        if (content != null) {
            note.setContent(content);
        }
        if (isPublic != null) {
            note.setIsPublic(isPublic ? 1 : 0);
        }

        noteMapper.updateById(note);
    }

    /**
     * 删除笔记
     */
    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        StudyNote note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of("笔记不存在");
        }

        if (!note.getUserId().equals(userId)) {
            throw BusinessException.of("无权删除此笔记");
        }

        noteMapper.deleteById(noteId);
    }

    /**
     * 获取用户笔记列表
     */
    public List<Map<String, Object>> getUserNotes(Long userId) {
        List<StudyNote> notes = noteMapper.selectList(
                new LambdaQueryWrapper<StudyNote>()
                        .eq(StudyNote::getUserId, userId)
                        .orderByDesc(StudyNote::getUpdatedAt)
        );

        return notes.stream()
                .map(note -> convertToMap(note, userId))
                .collect(Collectors.toList());
    }

    /**
     * 获取公开笔记列表
     */
    public Page<Map<String, Object>> getPublicNotes(Long subjectId, Integer page, Integer size, Long userId) {
        Page<StudyNote> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<StudyNote> wrapper = new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getIsPublic, 1)
                .orderByDesc(StudyNote::getLikeCount)
                .orderByDesc(StudyNote::getCreatedAt);

        if (subjectId != null) {
            wrapper.eq(StudyNote::getSubjectId, subjectId);
        }

        Page<StudyNote> notePage = noteMapper.selectPage(pageParam, wrapper);

        Page<Map<String, Object>> resultPage = new Page<>(page, size, notePage.getTotal());
        resultPage.setRecords(notePage.getRecords().stream()
                .map(note -> convertToMap(note, userId))
                .collect(Collectors.toList()));

        return resultPage;
    }

    /**
     * 获取笔记详情
     */
    @Transactional
    public Map<String, Object> getNoteDetail(Long noteId, Long currentUserId) {
        StudyNote note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of("笔记不存在");
        }

        // 检查权限
        if (note.getIsPublic() == 0 && !note.getUserId().equals(currentUserId)) {
            throw BusinessException.of("无权查看此笔记");
        }

        // 检查是否已浏览过，未浏览则增加浏览量
        if (currentUserId != null) {
            LambdaQueryWrapper<ViewRecord> viewWrapper = new LambdaQueryWrapper<ViewRecord>()
                    .eq(ViewRecord::getUserId, currentUserId)
                    .eq(ViewRecord::getTargetType, "NOTE")
                    .eq(ViewRecord::getTargetId, noteId);

            ViewRecord existingView = viewRecordMapper.selectOne(viewWrapper);
            if (existingView == null) {
                note.setViewCount(note.getViewCount() + 1);
                noteMapper.updateById(note);

                ViewRecord viewRecord = new ViewRecord();
                viewRecord.setUserId(currentUserId);
                viewRecord.setTargetType("NOTE");
                viewRecord.setTargetId(noteId);
                viewRecordMapper.insert(viewRecord);
            }
        } else {
            note.setViewCount(note.getViewCount() + 1);
            noteMapper.updateById(note);
        }

        Map<String, Object> map = convertToMap(note, true, currentUserId);
        return map;
    }

    /**
     * 点赞/取消点赞笔记
     * @return true表示点赞成功，false表示取消点赞
     */
    @Transactional
    public boolean toggleLike(Long userId, Long noteId) {
        StudyNote note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of("笔记不存在");
        }

        // 检查是否已点赞
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, "NOTE")
                .eq(LikeRecord::getTargetId, noteId);

        LikeRecord existingLike = likeRecordMapper.selectOne(wrapper);

        if (existingLike != null) {
            // 已点赞，取消点赞
            likeRecordMapper.deleteById(existingLike.getId());
            note.setLikeCount(Math.max(0, note.getLikeCount() - 1));
            noteMapper.updateById(note);
            return false;
        } else {
            // 未点赞，添加点赞
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setUserId(userId);
            likeRecord.setTargetType("NOTE");
            likeRecord.setTargetId(noteId);
            likeRecordMapper.insert(likeRecord);

            note.setLikeCount(note.getLikeCount() + 1);
            noteMapper.updateById(note);
            return true;
        }
    }

    /**
     * 检查是否已点赞
     */
    public boolean isLiked(Long userId, Long noteId) {
        if (userId == null) return false;
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, "NOTE")
                .eq(LikeRecord::getTargetId, noteId);
        return likeRecordMapper.selectCount(wrapper) > 0;
    }

    /**
     * 收藏笔记
     */
    @Transactional
    public void collectNote(Long noteId) {
        StudyNote note = noteMapper.selectById(noteId);
        if (note != null) {
            note.setCollectCount(note.getCollectCount() + 1);
            noteMapper.updateById(note);
        }
    }

    /**
     * 搜索笔记
     */
    public List<Map<String, Object>> searchNotes(String keyword, Integer limit) {
        List<StudyNote> notes = noteMapper.selectList(
                new LambdaQueryWrapper<StudyNote>()
                        .eq(StudyNote::getIsPublic, 1)
                        .like(StudyNote::getTitle, keyword)
                        .or()
                        .like(StudyNote::getContent, keyword)
                        .last("LIMIT " + limit)
        );

        return notes.stream()
                .map(note -> convertToMap(note, (Long) null))
                .collect(Collectors.toList());
    }

    /**
     * 转换笔记为Map
     */
    private Map<String, Object> convertToMap(StudyNote note, Long userId) {
        return convertToMap(note, false, userId);
    }

    private Map<String, Object> convertToMap(StudyNote note, boolean includeContent, Long userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", note.getId());
        map.put("title", note.getTitle());
        map.put("subjectId", note.getSubjectId());
        map.put("knowledgeId", note.getKnowledgeId());
        map.put("isPublic", note.getIsPublic());
        map.put("viewCount", note.getViewCount());
        map.put("likeCount", note.getLikeCount());
        map.put("collectCount", note.getCollectCount());
        map.put("createdAt", note.getCreatedAt());
        map.put("updatedAt", note.getUpdatedAt());

        if (includeContent) {
            map.put("content", note.getContent());
        }

        // 添加点赞状态
        if (userId != null) {
            map.put("liked", isLiked(userId, note.getId()));
        } else {
            map.put("liked", false);
        }

        User user = userMapper.selectById(note.getUserId());
        if (user != null) {
            map.put("authorName", user.getNickname());
            map.put("authorAvatar", user.getAvatar());
        }

        return map;
    }
}
