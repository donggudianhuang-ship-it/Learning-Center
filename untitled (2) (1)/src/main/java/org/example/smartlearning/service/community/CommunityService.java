package org.example.smartlearning.service.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.entity.*;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 社区服务类
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final List<String> SENSITIVE_WORDS = List.of("作弊", "代考", "辱骂", "广告", "刷单");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\u4e00-\\u9fa5A-Za-z0-9_]{1,20})");

    private final CommunityPostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final ViewRecordMapper viewRecordMapper;

    /**
     * 创建帖子
     */
    @Transactional
    public CommunityPost createPost(Long userId, String title, String content, Long subjectId, boolean anonymous) {
        validateContent(title, content);

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setSubjectId(subjectId);
        post.setAnonymous(anonymous ? 1 : 0);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(1);

        postMapper.insert(post);
        return post;
    }

    /**
     * 获取帖子列表（公开）
     */
    public Page<Map<String, Object>> getPublicPosts(Long subjectId, Integer page, Integer size, Long userId) {
        Page<CommunityPost> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, 1)
                .orderByDesc(CommunityPost::getCreatedAt);

        if (subjectId != null) {
            wrapper.eq(CommunityPost::getSubjectId, subjectId);
        }

        Page<CommunityPost> postPage = postMapper.selectPage(pageParam, wrapper);

        Page<Map<String, Object>> resultPage = new Page<>(page, size, postPage.getTotal());
        resultPage.setRecords(postPage.getRecords().stream()
                .map(post -> {
                    Map<String, Object> map = convertToMap(post);
                    // 添加点赞状态
                    if (userId != null) {
                        map.put("liked", isLiked(userId, "POST", post.getId()));
                    } else {
                        map.put("liked", false);
                    }
                    return map;
                })
                .collect(Collectors.toList()));

        return resultPage;
    }

    /**
     * 获取帖子详情（带浏览量控制）
     */
    @Transactional
    public Map<String, Object> getPostDetail(Long postId, Long userId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.of("帖子不存在");
        }

        // 检查是否已浏览过，未浏览则增加浏览量
        if (userId != null) {
            LambdaQueryWrapper<ViewRecord> viewWrapper = new LambdaQueryWrapper<ViewRecord>()
                    .eq(ViewRecord::getUserId, userId)
                    .eq(ViewRecord::getTargetType, "POST")
                    .eq(ViewRecord::getTargetId, postId);

            ViewRecord existingView = viewRecordMapper.selectOne(viewWrapper);
            if (existingView == null) {
                // 首次浏览，增加浏览量并记录
                post.setViewCount(post.getViewCount() + 1);
                postMapper.updateById(post);

                ViewRecord viewRecord = new ViewRecord();
                viewRecord.setUserId(userId);
                viewRecord.setTargetType("POST");
                viewRecord.setTargetId(postId);
                viewRecordMapper.insert(viewRecord);
            }
        } else {
            // 未登录用户，每次都增加浏览量
            post.setViewCount(post.getViewCount() + 1);
            postMapper.updateById(post);
        }

        Map<String, Object> map = convertToMap(post);

        // 检查当前用户是否已点赞
        if (userId != null) {
            boolean liked = isLiked(userId, "POST", postId);
            map.put("liked", liked);
        } else {
            map.put("liked", false);
        }

        // 获取评论
        List<PostComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .orderByAsc(PostComment::getCreatedAt)
        );

        List<Map<String, Object>> commentMaps = comments.stream()
                .map(this::convertCommentToMap)
                .collect(Collectors.toList());

        map.put("comments", commentMaps);
        return map;
    }

    /**
     * 添加评论
     */
    @Transactional
    public PostComment addComment(Long userId, Long postId, Long parentId, String content) {
        validateContent(content);

        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.of("帖子不存在");
        }

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setContent(content);
        comment.setLikeCount(0);

        commentMapper.insert(comment);

        // 更新评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        return comment;
    }

    /**
     * 点赞/取消点赞帖子
     * @return true表示点赞成功，false表示取消点赞
     */
    @Transactional
    public boolean toggleLike(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.of("帖子不存在");
        }

        // 检查是否已点赞
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, "POST")
                .eq(LikeRecord::getTargetId, postId);

        LikeRecord existingLike = likeRecordMapper.selectOne(wrapper);

        if (existingLike != null) {
            // 已点赞，取消点赞
            likeRecordMapper.deleteById(existingLike.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postMapper.updateById(post);
            return false;
        } else {
            // 未点赞，添加点赞
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setUserId(userId);
            likeRecord.setTargetType("POST");
            likeRecord.setTargetId(postId);
            likeRecordMapper.insert(likeRecord);

            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            return true;
        }
    }

    /**
     * 检查是否已点赞
     */
    public boolean isLiked(Long userId, String targetType, Long targetId) {
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetType, targetType)
                .eq(LikeRecord::getTargetId, targetId);
        return likeRecordMapper.selectCount(wrapper) > 0;
    }

    /**
     * 删除帖子
     */
    @Transactional
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.of("帖子不存在");
        }

        if (!post.getUserId().equals(userId)) {
            throw BusinessException.of("无权删除此帖子");
        }

        post.setStatus(0);
        postMapper.updateById(post);
    }

    /**
     * 获取用户的帖子
     */
    public List<Map<String, Object>> getUserPosts(Long userId) {
        List<CommunityPost> posts = postMapper.selectList(
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getUserId, userId)
                        .eq(CommunityPost::getStatus, 1)
                        .orderByDesc(CommunityPost::getCreatedAt)
        );

        return posts.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * 转换帖子为Map（处理匿名）
     */
    private Map<String, Object> convertToMap(CommunityPost post) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", post.getId());
        map.put("title", post.getTitle());
        map.put("content", post.getContent());
        map.put("subjectId", post.getSubjectId());
        map.put("viewCount", post.getViewCount());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        map.put("createdAt", post.getCreatedAt());

        // 处理匿名显示
        if (post.getAnonymous() == 1) {
            map.put("authorName", "匿名用户");
            map.put("authorAvatar", null);
        } else {
            User user = userMapper.selectById(post.getUserId());
            if (user != null) {
                map.put("authorName", user.getNickname());
                map.put("authorAvatar", user.getAvatar());
            }
        }

        return map;
    }

    /**
     * 转换评论为Map
     */
    private Map<String, Object> convertCommentToMap(PostComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("postId", comment.getPostId());
        map.put("parentId", comment.getParentId());
        map.put("content", comment.getContent());
        map.put("likeCount", comment.getLikeCount());
        map.put("createdAt", comment.getCreatedAt());
        map.put("mentions", extractMentions(comment.getContent()));

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            map.put("authorName", user.getNickname());
            map.put("authorAvatar", user.getAvatar());
        }

        return map;
    }

    private void validateContent(String... contents) {
        for (String content : contents) {
            if (content == null) {
                continue;
            }
            String normalized = content.toLowerCase();
            for (String word : SENSITIVE_WORDS) {
                if (normalized.contains(word.toLowerCase())) {
                    throw BusinessException.of("内容包含敏感词，请修改后再发布");
                }
            }
        }
    }

    private List<String> extractMentions(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        return matcher.results()
                .map(match -> match.group(1))
                .distinct()
                .collect(Collectors.toList());
    }
}
