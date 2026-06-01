package org.example.smartlearning.service.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.CommentResponse;
import org.example.smartlearning.entity.CommunityPost;
import org.example.smartlearning.entity.PostComment;
import org.example.smartlearning.entity.User;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.CommunityPostMapper;
import org.example.smartlearning.mapper.PostCommentMapper;
import org.example.smartlearning.mapper.UserMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 评论服务 —— CRUD + 评论限流
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int RATE_LIMIT = 10;
    private static final long RATE_WINDOW = 60;
    private static final String RATE_KEY = "community:rate:comment:";

    private final PostCommentMapper commentMapper;
    private final CommunityPostMapper postMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommunityHelper helper;

    @Transactional
    public CommentResponse addComment(Long userId, Long postId, Long parentId, String content) {
        checkCommentRateLimit(userId);
        helper.validateContent(content);

        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw BusinessException.of("帖子不存在");

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setContent(content);
        comment.setLikeCount(0);
        commentMapper.insert(comment);

        postMapper.incrementCommentCount(postId);

        User user = userMapper.selectById(userId);
        String parentAuthorName = null;
        if (parentId != null && parentId > 0) {
            parentAuthorName = loadCommentAuthorName(parentId);
        }
        return helper.toCommentResponse(comment, user, parentAuthorName, false);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw BusinessException.of("评论不存在");
        if (!comment.getUserId().equals(userId)) throw BusinessException.of("只能删除自己的评论");

        commentMapper.deleteById(commentId);
        postMapper.decrementCommentCount(comment.getPostId());
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long postId, Integer page, Integer size, Long userId) {
        Page<PostComment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PostComment> wrapper = new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, postId)
                .orderByAsc(PostComment::getCreatedAt);
        Page<PostComment> commentPage = commentMapper.selectPage(pageParam, wrapper);

        if (commentPage.getRecords().isEmpty()) {
            Page<CommentResponse> empty = new Page<>(page, size, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        List<CommentResponse> records = helper.buildCommentResponses(commentPage.getRecords(), userId);
        Page<CommentResponse> result = new Page<>(page, size, commentPage.getTotal());
        result.setRecords(records);
        return result;
    }

    // ── 限流 ──

    private void checkCommentRateLimit(Long userId) {
        String key = RATE_KEY + userId;
        try {
            Long c = redisTemplate.opsForValue().increment(key);
            if (c != null && c == 1) redisTemplate.expire(key, RATE_WINDOW, TimeUnit.SECONDS);
            if (c != null && c > RATE_LIMIT)
                throw BusinessException.of("评论过于频繁，请稍后再试（每分钟最多" + RATE_LIMIT + "条）");
        } catch (BusinessException e) { throw e; } catch (Exception ignored) {}
    }

    private String loadCommentAuthorName(Long commentId) {
        PostComment parent = commentMapper.selectById(commentId);
        if (parent != null) {
            User author = userMapper.selectById(parent.getUserId());
            return author != null ? author.getNickname() : "用户";
        }
        return null;
    }
}
