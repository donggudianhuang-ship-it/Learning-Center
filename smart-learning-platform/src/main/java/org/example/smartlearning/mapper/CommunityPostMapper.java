package org.example.smartlearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.example.smartlearning.entity.CommunityPost;

@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {

    /** 原子递增浏览量 */
    @Update("UPDATE community_post SET view_count = view_count + 1 WHERE id = #{postId}")
    int incrementViewCount(Long postId);

    /** 原子递增点赞数 */
    @Update("UPDATE community_post SET like_count = like_count + 1 WHERE id = #{postId}")
    int incrementLikeCount(Long postId);

    /** 原子递减点赞数（不低于0） */
    @Update("UPDATE community_post SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{postId}")
    int decrementLikeCount(Long postId);

    /** 原子递增评论数 */
    @Update("UPDATE community_post SET comment_count = comment_count + 1 WHERE id = #{postId}")
    int incrementCommentCount(Long postId);

    /** 原子递减评论数 */
    @Update("UPDATE community_post SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = #{postId}")
    int decrementCommentCount(Long postId);
}
