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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子服务 —— CRUD + Redis ZSet 热榜 + 列表缓存 + 发帖限流
 */
@Service
@RequiredArgsConstructor
public class PostService {

    // —— Redis Key ——
    private static final String CACHE_KEY_SET = "community:posts:cache-keys";   // Set: 追踪所有缓存 key
    private static final String CACHE_PREFIX  = "community:posts:page:";
    private static final long   CACHE_TTL     = 60;
    private static final String HOT_ZSET      = "community:hot:posts";          // ZSet: 热榜
    private static final String HOT_ZSET_SUBJ = "community:hot:posts:subject:"; // ZSet: 分科热榜

    // —— 限流 ——
    private static final int    RATE_LIMIT  = 3;
    private static final long   RATE_WINDOW = 60;
    private static final String RATE_KEY    = "community:rate:post:";

    // —— 热度权重：热门 = 点赞数排序 ——
    private static final int SCORE_LIKE = 1;

    private final CommunityPostMapper postMapper;
    private final UserMapper          userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommunityHelper     helper;
    private final MediaService        mediaService;
    private final CommunitySectionMapper sectionMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════════════════════════════════════════
    //  公开 API
    // ═══════════════════════════════════════════════

    @Transactional
    public CommunityPostResponse createPost(Long userId, String title, String content,
                                             Long subjectId, Boolean anonymous, List<String> mediaUrls,
                                             Long sectionId) {
        checkPostRateLimit(userId);
        helper.validateContent(title, content);

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setSubjectId(subjectId);
        post.setSectionId(sectionId);
        post.setAnonymous(anonymous != null && anonymous ? 1 : 0);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(1);
        postMapper.insert(post);

        mediaService.bindMediaUrls(post.getId(), mediaUrls);

        // 新帖子加入热榜 ZSet（初始 0 分）
        addToHotZset(post.getId(), subjectId);

        evictCachedPages();

        User user = userMapper.selectById(userId);
        List<org.example.smartlearning.dto.response.PostMediaResponse> medias =
                mediaService.getPostMedia(post.getId()).stream()
                        .map(mediaService::toResponse).collect(Collectors.toList());
        return helper.toPostResponse(post, user, false, false, medias);
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
        List<org.example.smartlearning.dto.response.PostMediaResponse> medias =
                mediaService.getPostMedia(postId).stream()
                        .map(mediaService::toResponse).collect(Collectors.toList());
        return helper.toPostResponse(post, user,
                helper.isUserLiked(userId, "POST", postId),
                helper.isUserCollected(userId, "POST", postId), medias);
    }

    @Transactional(readOnly = true)
    public Page<CommunityPostResponse> getPublicPosts(Long subjectId, Integer page, Integer size,
                                                       String sort, Long userId, String keyword,
                                                       Long sectionId) {
        boolean isHot = "hot".equals(sort);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        // 最新第一页走 String 缓存
        if (!hasKeyword && !isHot && page == 1 && size <= 20) {
            String key = cacheKey(subjectId, page, size);
            Page<CommunityPostResponse> cached = getCachedPostList(key, userId);
            if (cached != null) return cached;
        }

        // 热门走 ZSet
        if (isHot && !hasKeyword) {
            Page<CommunityPostResponse> result = getHotPostsFromZSet(subjectId, page, size, userId);
            if (result != null) return result;
        }

        // 回退到 DB 查询（全文搜索 或 ZSet 失效时）
        Page<CommunityPost> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, 1);
        if (subjectId != null) wrapper.eq(CommunityPost::getSubjectId, subjectId);
        if (sectionId != null) wrapper.eq(CommunityPost::getSectionId, sectionId);

        if (hasKeyword) {
            String safe = keyword.replace("'", "''");
            wrapper.apply("MATCH(title, content) AGAINST({0} IN NATURAL LANGUAGE MODE)", keyword);
            wrapper.last("ORDER BY MATCH(title, content) AGAINST('" + safe + "') DESC, created_at DESC");
        } else {
            wrapper.orderByDesc(CommunityPost::getCreatedAt);
        }

        Page<CommunityPost> postPage = postMapper.selectPage(pageParam, wrapper);
        if (postPage.getRecords().isEmpty()) {
            Page<CommunityPostResponse> empty = new Page<>(page, size, postPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        Page<CommunityPostResponse> result = buildPostListPage(postPage.getRecords(), postPage.getTotal(), page, size, userId);

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

        List<org.example.smartlearning.dto.response.PostMediaResponse> medias =
                mediaService.getPostMedia(postId).stream()
                        .map(mediaService::toResponse).collect(Collectors.toList());
        CommunityPostResponse postResp = helper.toPostResponse(post, postAuthor, liked, collected, medias);
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
        mediaService.deletePostMedia(postId);

        // 从热榜移除
        removeFromHotZset(postId, post.getSubjectId());

        evictCachedPages();
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getUserPosts(Long userId) {
        List<CommunityPost> posts = postMapper.selectList(
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getUserId, userId)
                        .eq(CommunityPost::getStatus, 1)
                        .orderByDesc(CommunityPost::getCreatedAt));
        User user = userMapper.selectById(userId);
        return posts.stream().map(p -> helper.toPostResponse(p, user, false, false, Collections.emptyList())).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════
    //  热榜 ZSet（供 InteractionService / CommentService 调用）
    // ═══════════════════════════════════════════════

    /** 点赞时调用：likeDelta +1 或 -1 */
    public void updateHotScore(Long postId, int likeDelta) {
        if (likeDelta == 0) return;
        CommunityPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) return;
        try {
            redisTemplate.opsForZSet().incrementScore(HOT_ZSET, postId.toString(), likeDelta);
            if (post.getSubjectId() != null) {
                redisTemplate.opsForZSet().incrementScore(HOT_ZSET_SUBJ + post.getSubjectId(), postId.toString(), likeDelta);
            }
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════
    //  缓存
    // ═══════════════════════════════════════════════

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
            // 追踪 key，避免使用 KEYS *
            redisTemplate.opsForSet().add(CACHE_KEY_SET, key);
            redisTemplate.expire(CACHE_KEY_SET, 1, TimeUnit.HOURS);
        } catch (Exception ignored) {}
    }

    /** 用 Set 追踪替代 KEYS *，O(N) 只扫描缓存 key，不会阻塞整个 Redis */
    private void evictCachedPages() {
        try {
            Set<Object> keys = redisTemplate.opsForSet().members(CACHE_KEY_SET);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys.stream().map(Object::toString).collect(Collectors.toSet()));
                redisTemplate.delete(CACHE_KEY_SET);
            }
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════
    //  热榜 ZSet 内部
    // ═══════════════════════════════════════════════

    private void addToHotZset(Long postId, Long subjectId) {
        try {
            redisTemplate.opsForZSet().add(HOT_ZSET, postId.toString(), 0);
            if (subjectId != null) {
                redisTemplate.opsForZSet().add(HOT_ZSET_SUBJ + subjectId, postId.toString(), 0);
            }
        } catch (Exception ignored) {}
    }

    private void removeFromHotZset(Long postId, Long subjectId) {
        try {
            redisTemplate.opsForZSet().remove(HOT_ZSET, postId.toString());
            if (subjectId != null) {
                redisTemplate.opsForZSet().remove(HOT_ZSET_SUBJ + subjectId, postId.toString());
            }
        } catch (Exception ignored) {}
    }

    /**
     * 从 Redis ZSet 获取热门帖子，再用 WHERE id IN (...) 走主键索引
     */
    private Page<CommunityPostResponse> getHotPostsFromZSet(Long subjectId, int page, int size, Long userId) {
        String zsetKey = subjectId != null ? HOT_ZSET_SUBJ + subjectId : HOT_ZSET;
        try {
            int start = (page - 1) * size;
            int end = start + size - 1;
            Set<Object> idSet = redisTemplate.opsForZSet().reverseRange(zsetKey, start, end);
            if (idSet == null || idSet.isEmpty()) return null;

            List<Long> postIds = idSet.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
            List<CommunityPost> posts = postMapper.selectBatchIds(postIds);
            if (posts.isEmpty()) return null;

            // 按 ZSet 顺序排列
            Map<Long, CommunityPost> postMap = posts.stream()
                    .collect(Collectors.toMap(CommunityPost::getId, p -> p));
            List<CommunityPost> ordered = postIds.stream()
                    .map(postMap::get).filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 总数
            Long total = redisTemplate.opsForZSet().size(zsetKey);
            if (total == null) total = 0L;

            return buildPostListPage(ordered, total, page, size, userId);
        } catch (Exception e) {
            return null; // 回退到 DB 查询
        }
    }

    // ═══════════════════════════════════════════════
    //  限流
    // ═══════════════════════════════════════════════

    private void checkPostRateLimit(Long userId) {
        String key = RATE_KEY + userId;
        try {
            Long c = redisTemplate.opsForValue().increment(key);
            if (c != null && c == 1) redisTemplate.expire(key, RATE_WINDOW, TimeUnit.SECONDS);
            if (c != null && c > RATE_LIMIT)
                throw BusinessException.of("发帖过于频繁，请稍后再试（每分钟最多" + RATE_LIMIT + "条）");
        } catch (BusinessException e) { throw e; } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════
    //  工具
    // ═══════════════════════════════════════════════

    private Map<Long, String> buildSectionNameMap(List<CommunityPost> posts) {
        Set<Long> sectionIds = posts.stream()
                .map(CommunityPost::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (sectionIds.isEmpty()) return Collections.emptyMap();
        return sectionMapper.selectBatchIds(sectionIds).stream()
                .collect(Collectors.toMap(
                        org.example.smartlearning.entity.CommunitySection::getId,
                        org.example.smartlearning.entity.CommunitySection::getName));
    }

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
        Map<Long, List<org.example.smartlearning.dto.response.PostMediaResponse>> mediaMap = mediaService.batchLoadMedia(postIds);
        Map<Long, String> sectionNameMap = buildSectionNameMap(posts);

        List<CommunityPostResponse> records = posts.stream()
                .map(p -> {
                    CommunityPostResponse r = helper.toPostResponse(p, userMap.get(p.getUserId()),
                            liked.contains(p.getId()), collected.contains(p.getId()),
                            mediaMap.getOrDefault(p.getId(), Collections.emptyList()));
                    r.setSectionName(sectionNameMap.get(p.getSectionId()));
                    return r;
                })
                .collect(Collectors.toList());
        Page<CommunityPostResponse> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }
}
