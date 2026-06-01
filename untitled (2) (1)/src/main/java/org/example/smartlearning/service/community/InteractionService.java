package org.example.smartlearning.service.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.CommunityPostResponse;
import org.example.smartlearning.entity.*;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 互动服务 —— 点赞 + 收藏 + 收藏列表
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final LikeRecordMapper likeRecordMapper;
    private final CollectRecordMapper collectRecordMapper;
    private final CommunityPostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final CommunityHelper helper;

    @Transactional
    public Map<String, Object> toggleLike(Long userId, String targetType, Long targetId) {
        validateTargetExists(targetType, targetId);

        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, targetType)
                .eq(LikeRecord::getTargetId, targetId);

        LikeRecord existing = likeRecordMapper.selectOne(wrapper);

        if (existing != null) {
            likeRecordMapper.deleteById(existing.getId());
            if ("POST".equals(targetType)) postMapper.decrementLikeCount(targetId);
            else commentMapper.decrementLikeCount(targetId);
            return Map.of("liked", false, "message", "取消点赞");
        } else {
            LikeRecord r = new LikeRecord();
            r.setUserId(userId); r.setTargetType(targetType); r.setTargetId(targetId);
            likeRecordMapper.insert(r);
            if ("POST".equals(targetType)) postMapper.incrementLikeCount(targetId);
            else commentMapper.incrementLikeCount(targetId);
            return Map.of("liked", true, "message", "点赞成功");
        }
    }

    @Transactional
    public Map<String, Object> toggleCollect(Long userId, String targetType, Long targetId) {
        LambdaQueryWrapper<CollectRecord> wrapper = new LambdaQueryWrapper<CollectRecord>()
                .eq(CollectRecord::getUserId, userId)
                .eq(CollectRecord::getTargetType, targetType)
                .eq(CollectRecord::getTargetId, targetId);

        CollectRecord existing = collectRecordMapper.selectOne(wrapper);

        if (existing != null) {
            collectRecordMapper.deleteById(existing.getId());
            return Map.of("collected", false, "message", "取消收藏");
        } else {
            CollectRecord r = new CollectRecord();
            r.setUserId(userId); r.setTargetType(targetType); r.setTargetId(targetId);
            collectRecordMapper.insert(r);
            return Map.of("collected", true, "message", "收藏成功");
        }
    }

    @Transactional(readOnly = true)
    public Page<CommunityPostResponse> getUserCollections(Long userId, Integer page, Integer size) {
        Page<CollectRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CollectRecord> wrapper = new LambdaQueryWrapper<CollectRecord>()
                .eq(CollectRecord::getUserId, userId)
                .eq(CollectRecord::getTargetType, "POST")
                .orderByDesc(CollectRecord::getCreatedAt);
        Page<CollectRecord> collectPage = collectRecordMapper.selectPage(pageParam, wrapper);

        List<Long> postIds = collectPage.getRecords().stream()
                .map(CollectRecord::getTargetId).collect(Collectors.toList());

        if (postIds.isEmpty()) {
            Page<CommunityPostResponse> empty = new Page<>(page, size, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        List<CommunityPost> posts = postMapper.selectBatchIds(postIds);
        Map<Long, CommunityPost> postMap = posts.stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p));

        Set<Long> authorIds = posts.stream().filter(p -> p.getAnonymous() == 0)
                .map(CommunityPost::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = helper.loadUserMap(authorIds);
        Set<Long> liked = helper.loadUserLikedTargets(userId, "POST", postIds);

        List<CommunityPostResponse> records = postIds.stream()
                .map(postMap::get).filter(Objects::nonNull)
                .map(p -> helper.toPostResponse(p, userMap.get(p.getUserId()),
                        liked.contains(p.getId()), true))
                .collect(Collectors.toList());

        Page<CommunityPostResponse> result = new Page<>(page, size, collectPage.getTotal());
        result.setRecords(records);
        return result;
    }

    private void validateTargetExists(String targetType, Long targetId) {
        switch (targetType) {
            case "POST" -> { if (postMapper.selectById(targetId) == null) throw BusinessException.of("帖子不存在"); }
            case "COMMENT" -> { if (commentMapper.selectById(targetId) == null) throw BusinessException.of("评论不存在"); }
            default -> throw BusinessException.of("不支持的目标类型: " + targetType);
        }
    }
}
