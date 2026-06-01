package org.example.smartlearning.service.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.CommentResponse;
import org.example.smartlearning.dto.response.CommunityPostResponse;
import org.example.smartlearning.entity.LikeRecord;
import org.example.smartlearning.entity.PostComment;
import org.example.smartlearning.entity.User;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 社区共享工具 —— 用户查询、点赞/收藏状态、DTO转换、敏感词过滤
 */
@Component
@RequiredArgsConstructor
public class CommunityHelper {

    private static final List<String> SENSITIVE_WORDS = List.of("作弊", "代考", "辱骂", "广告", "刷单");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\u4e00-\\u9fa5A-Za-z0-9_]{1,20})");

    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final CollectRecordMapper collectRecordMapper;
    // 仅 buildCommentResponses 需要
    private final PostCommentMapper commentMapper;

    // ── 批量查询 ──

    Map<Long, User> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    Set<Long> loadUserLikedTargets(Long userId, String targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        return likeRecordMapper.selectList(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, userId)
                        .eq(LikeRecord::getTargetType, targetType)
                        .in(LikeRecord::getTargetId, targetIds)
        ).stream().map(LikeRecord::getTargetId).collect(Collectors.toSet());
    }

    Set<Long> loadUserCollectedTargets(Long userId, String targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        return collectRecordMapper.selectList(
                new LambdaQueryWrapper<org.example.smartlearning.entity.CollectRecord>()
                        .eq(org.example.smartlearning.entity.CollectRecord::getUserId, userId)
                        .eq(org.example.smartlearning.entity.CollectRecord::getTargetType, targetType)
                        .in(org.example.smartlearning.entity.CollectRecord::getTargetId, targetIds)
        ).stream().map(org.example.smartlearning.entity.CollectRecord::getTargetId).collect(Collectors.toSet());
    }

    boolean isUserLiked(Long userId, String targetType, Long targetId) {
        return likeRecordMapper.selectCount(
                new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, userId)
                        .eq(LikeRecord::getTargetType, targetType)
                        .eq(LikeRecord::getTargetId, targetId)
        ) > 0;
    }

    boolean isUserCollected(Long userId, String targetType, Long targetId) {
        return collectRecordMapper.selectCount(
                new LambdaQueryWrapper<org.example.smartlearning.entity.CollectRecord>()
                        .eq(org.example.smartlearning.entity.CollectRecord::getUserId, userId)
                        .eq(org.example.smartlearning.entity.CollectRecord::getTargetType, targetType)
                        .eq(org.example.smartlearning.entity.CollectRecord::getTargetId, targetId)
        ) > 0;
    }

    // ── 评论批量组装 ──

    /**
     * 批量构建评论响应列表，高效加载作者和被回复者信息
     */
    List<CommentResponse> buildCommentResponses(List<PostComment> comments, Long userId) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查作者
        Set<Long> authorIds = comments.stream().map(PostComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = loadUserMap(authorIds);

        // 批量查点赞
        Set<Long> likedIds = userId != null
                ? loadUserLikedTargets(userId, "COMMENT",
                        comments.stream().map(PostComment::getId).collect(Collectors.toList()))
                : Collections.emptySet();

        // 批量查被回复者
        List<Long> parentIds = comments.stream()
                .map(PostComment::getParentId).filter(pid -> pid != null && pid > 0)
                .distinct().collect(Collectors.toList());

        Map<Long, String> parentAuthorNameMap = Collections.emptyMap();
        if (!parentIds.isEmpty()) {
            List<PostComment> parentComments = commentMapper.selectBatchIds(parentIds);
            if (!parentComments.isEmpty()) {
                Set<Long> parentUserIds = parentComments.stream()
                        .map(PostComment::getUserId).collect(Collectors.toSet());
                Map<Long, User> parentUserMap = loadUserMap(parentUserIds);
                parentAuthorNameMap = parentComments.stream()
                        .collect(Collectors.toMap(
                                PostComment::getId,
                                pc -> {
                                    User u = parentUserMap.get(pc.getUserId());
                                    return u != null ? u.getNickname() : "用户";
                                }));
            }
        }
        final Map<Long, String> finalMap = parentAuthorNameMap;

        return comments.stream()
                .map(c -> {
                    User author = userMap.get(c.getUserId());
                    return toCommentResponse(c, author, finalMap.get(c.getParentId()),
                            likedIds.contains(c.getId()));
                })
                .collect(Collectors.toList());
    }

    /**
     * 按帖子ID加载评论（给 PostService 用，它没有 commentMapper）
     */
    List<CommentResponse> buildCommentResponsesForPost(Long postId, Long userId) {
        List<PostComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .orderByAsc(PostComment::getCreatedAt));
        return buildCommentResponses(comments, userId);
    }

    // ── DTO 转换 ──

    CommunityPostResponse toPostResponse(org.example.smartlearning.entity.CommunityPost post,
                                          User author, boolean liked, boolean collected) {
        CommunityPostResponse.CommunityPostResponseBuilder builder = CommunityPostResponse.builder()
                .id(post.getId()).title(post.getTitle()).content(post.getContent())
                .subjectId(post.getSubjectId()).anonymous(post.getAnonymous())
                .viewCount(post.getViewCount()).likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount()).status(post.getStatus())
                .liked(liked).collected(collected)
                .createdAt(post.getCreatedAt()).updatedAt(post.getUpdatedAt());

        if (post.getAnonymous() == 1 || author == null) {
            builder.authorId(null).authorName("匿名用户").authorAvatar(null);
        } else {
            builder.authorId(author.getId())
                    .authorName(author.getNickname()).authorAvatar(author.getAvatar());
        }
        return builder.build();
    }

    CommentResponse toCommentResponse(PostComment comment, User author,
                                       String parentAuthorName, boolean liked) {
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId()).postId(comment.getPostId())
                .parentId(comment.getParentId()).content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .mentions(extractMentions(comment.getContent()))
                .parentAuthorName(parentAuthorName).liked(liked)
                .createdAt(comment.getCreatedAt());

        if (author != null) {
            builder.authorId(author.getId())
                    .authorName(author.getNickname()).authorAvatar(author.getAvatar());
        }
        return builder.build();
    }

    // ── 工具 ──

    List<String> extractMentions(String content) {
        if (content == null || content.isBlank()) return Collections.emptyList();
        return MENTION_PATTERN.matcher(content).results()
                .map(m -> m.group(1)).distinct().collect(Collectors.toList());
    }

    void validateContent(String... contents) {
        for (String content : contents) {
            if (content == null) continue;
            String n = content.toLowerCase();
            for (String word : SENSITIVE_WORDS) {
                if (n.contains(word.toLowerCase())) {
                    throw BusinessException.of("内容包含敏感词，请修改后再发布");
                }
            }
        }
    }
}
