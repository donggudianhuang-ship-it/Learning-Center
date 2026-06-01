package org.example.smartlearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.example.smartlearning.entity.PostComment;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {

    /** 原子递增点赞数 */
    @Update("UPDATE post_comment SET like_count = like_count + 1 WHERE id = #{commentId}")
    int incrementLikeCount(Long commentId);

    /** 原子递减点赞数 */
    @Update("UPDATE post_comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{commentId}")
    int decrementLikeCount(Long commentId);
}
