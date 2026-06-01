package org.example.smartlearning.service.community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.dto.response.CommentResponse;
import org.example.smartlearning.dto.response.CommunityPostResponse;
import org.example.smartlearning.dto.response.PostDetailResponse;
import org.example.smartlearning.entity.CommunityPost;
import org.example.smartlearning.entity.User;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子服务 —— CRUD + 列表缓存 + 发帖限流
 */
@Service
@RequiredArgsConstructor
public class PostService {

    // —— 缓存 ——
    private static final String CACHE_PREFIX = "community:posts:page:";
    private static final long CACHE_TTL = 60;

    // —— 限流 ——
    private static final int RATE_LIMIT = 3;
    private static final long RATE_WINDOW = 60;
    private static final String RATE_KEY = "community:rate:post:";

    // —— 排序 ——
    private static final String HOT_SORT =
            " ORDER BY (like_count * 3 + comment_count * 5) " +
            "/ POWER(TIMESTAMPDIFF(HOUR, created_at, NOW()) + 2, 1.5) DESC";

    private final CommunityPostMapper postMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommunityHelper helper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CommunityPostResponse createPost(Long userId, String title, String content,
                                             Long subjectId, Boolean anonymous) {
        checkPostRateLimit(userId);
        helper.validateContent(title, content);

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setSubjectId(subjectId);
        post.setAnonymous(anonymous != null && anonymous ? 1 : 0);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(1);
        postMapper.insert(post);

        evictPostListCache();

        User user = userMapper.selectById(userId);
        return helper.toPostResponse(post, user, false, false);
    }

    @Transactional
    public CommunityPostResponse updatePost(Long userId, Long postId, String title, String content) {
        helper.validateContent(title, content);
        CommunityPost post = requirePost(postId);
        if (!post.getUserId().equals(userId)) {
            throw BusinessException.of("只能编辑自己的帖子");
        }
        post.setTitle(title);
        post.setContent(content);
        postMapper.updateById(post);

        User user = userMapper.selectById(userId);
        return helper.toPostResponse(post, user,
                helper.isUserLiked(userId, "POST", postId),
                helper.isUserCollected(userId, "POST", postId));
    }

    @Transactional(readOnly = true)
    public Page<CommunityPostResponse> getPublicPosts(Long subjectId, Integer page, Integer size,
                                                       String sort, Long userId, String keyword) {
        boolean isHot = "hot".equals(sort);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        // 搜索不走缓存
        if (!hasKeyword && !isHot && page == 1 && size <= 20) {
            String key = cacheKey(subjectId, page, size);
            Page<CommunityPostResponse> cached = getCachedPostList(key, userId);
            if (cached != null) return cached;
        }

        Page<CommunityPost> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, 1);
        if (subjectId != null) wrapper.eq(CommunityPost::getSubjectId, subjectId);

        if (hasKeyword) {
            // 全文搜索：MATCH 条件 + 相关性优先排序
            String safe = keyword.replace("'", "''");
            wrapper.apply("MATCH(title, content) AGAINST({0} IN NATURAL LANGUAGE MODE)", keyword);
            if (isHot) {
                wrapper.last("ORDER BY MATCH(title, content) AGAINST('" + safe + "') DESC, " +
                        "(like_count * 3 + comment_count * 5) " +
                        "/ POWER(TIMESTAMPDIFF(HOUR, created_at, NOW()) + 2, 1.5) DESC");
            } else {
                wrapper.last("ORDER BY MATCH(title, content) AGAINST('" + safe + "') DESC, created_at DESC");
            }
        } else {
            if (isHot) wrapper.last(HOT_SORT);
            else wrapper.orderByDesc(CommunityPost::getCreatedAt);
        }

        Page<CommunityPost> postPage = postMapper.selectPage(pageParam, wrapper);
        List<CommunityPost> posts = postPage.getRecords();

        if (posts.isEmpty()) {
            Page<CommunityPostResponse> empty = new Page<>(page, size, postPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        Page<CommunityPostResponse> result = buildPostListPage(posts, postPage.getTotal(), page, size, userId);

        if (!hasKeyword && !isHot && page == 1 && size <= 20) {
            setCachedPostList(cacheKey(subjectId, page, size), result);
        }
        return result;
    }

    @Transactional
    public PostDetailResponse getPostDetail(Long postId, Long userId) {
        CommunityPost post = requirePost(postId);

        User postAuthor = null;
        if (post.getAnonymous() == 0) {
            postAuthor = userMapper.selectById(post.getUserId());
        }
        boolean liked = userId != null && helper.isUserLiked(userId, "POST", postId);
        boolean collected = userId != null && helper.isUserCollected(userId, "POST", postId);

        CommunityPostResponse postResp = helper.toPostResponse(post, postAuthor, liked, collected);
        List<CommentResponse> commentResp = helper.buildCommentResponsesForPost(postId, userId);

        return PostDetailResponse.builder().post(postResp).comments(commentResp).build();
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = requirePost(postId);
        if (!post.getUserId().equals(userId)) {
            throw BusinessException.of("无权删除此帖子");
        }
        post.setStatus(0);
        postMapper.updateById(post);
        evictPostListCache();
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getUserPosts(Long userId) {
        List<CommunityPost> posts = postMapper.selectList(
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getUserId, userId)
                        .eq(CommunityPost::getStatus, 1)
                        .orderByDesc(CommunityPost::getCreatedAt));
        User user = userMapper.selectById(userId);
        return posts.stream().map(p -> helper.toPostResponse(p, user, false, false)).collect(Collectors.toList());
    }

    // ── 缓存 ──

    private String cacheKey(Long subjectId, int page, int size) {
        return CACHE_PREFIX + (subjectId != null ? subjectId : "all") + ":" + page + ":" + size;
    }

    @SuppressWarnings("unchecked")
    private Page<CommunityPostResponse> getCachedPostList(String key, Long userId) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) return null;
            List<CommunityPostResponse> records;
            if (cached instanceof List) records = (List<CommunityPostResponse>) cached;
            else records = objectMapper.convertValue(cached, new TypeReference<List<CommunityPostResponse>>() {});

            if (userId != null && !records.isEmpty()) {
                List<Long> ids = records.stream().map(CommunityPostResponse::getId).collect(Collectors.toList());
                Set<Long> liked = helper.loadUserLikedTargets(userId, "POST", ids);
                Set<Long> collected = helper.loadUserCollectedTargets(userId, "POST", ids);
                records.forEach(r -> { r.setLiked(liked.contains(r.getId())); r.setCollected(collected.contains(r.getId())); });
            }
            Page<CommunityPostResponse> page = new Page<>(1, records.size(), records.size());
            page.setRecords(records);
            return page;
        } catch (Exception e) { return null; }
    }

    private void setCachedPostList(String key, Page<CommunityPostResponse> page) {
        try {
            List<CommunityPostResponse> base = page.getRecords().stream()
                    .map(r -> CommunityPostResponse.builder()
                            .id(r.getId()).title(r.getTitle()).content(r.getContent())
                            .subjectId(r.getSubjectId()).anonymous(r.getAnonymous())
                            .viewCount(r.getViewCount()).likeCount(r.getLikeCount())
                            .commentCount(r.getCommentCount()).status(r.getStatus())
                            .authorId(r.getAuthorId()).authorName(r.getAuthorName()).authorAvatar(r.getAuthorAvatar())
                            .liked(false).collected(false)
                            .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                            .build()).collect(Collectors.toList());
            redisTemplate.opsForValue().set(key, base, CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    private void evictPostListCache() {
        try { Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
              if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys); } catch (Exception ignored) {}
    }

    // ── 限流 ──

    private void checkPostRateLimit(Long userId) {
        String key = RATE_KEY + userId;
        try {
            Long c = redisTemplate.opsForValue().increment(key);
            if (c != null && c == 1) redisTemplate.expire(key, RATE_WINDOW, TimeUnit.SECONDS);
            if (c != null && c > RATE_LIMIT)
                throw BusinessException.of("发帖过于频繁，请稍后再试（每分钟最多" + RATE_LIMIT + "条）");
        } catch (BusinessException e) { throw e; } catch (Exception ignored) {}
    }

    // ── 工具 ──

    private CommunityPost requirePost(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw BusinessException.of("帖子不存在");
        return post;
    }

    private Page<CommunityPostResponse> buildPostListPage(List<CommunityPost> posts, long total,
                                                           int page, int size, Long userId) {
        Set<Long> authorIds = posts.stream().filter(p -> p.getAnonymous() == 0)
                .map(CommunityPost::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = helper.loadUserMap(authorIds);
        List<Long> postIds = posts.stream().map(CommunityPost::getId).collect(Collectors.toList());
        Set<Long> liked = userId != null ? helper.loadUserLikedTargets(userId, "POST", postIds) : Collections.emptySet();
        Set<Long> collected = userId != null ? helper.loadUserCollectedTargets(userId, "POST", postIds) : Collections.emptySet();

        List<CommunityPostResponse> records = posts.stream()
                .map(p -> helper.toPostResponse(p, userMap.get(p.getUserId()),
                        liked.contains(p.getId()), collected.contains(p.getId())))
                .collect(Collectors.toList());
        Page<CommunityPostResponse> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }
}
