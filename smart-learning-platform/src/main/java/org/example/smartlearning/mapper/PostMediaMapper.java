package org.example.smartlearning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.smartlearning.entity.PostMedia;

import java.util.Collections;
import java.util.List;

@Mapper
public interface PostMediaMapper extends BaseMapper<PostMedia> {

    @Select("SELECT * FROM post_media WHERE post_id = #{postId} ORDER BY sort_order")
    List<PostMedia> findByPostId(Long postId);

    /** 批量按帖子ID查媒体，用 MyBatis-Plus 的 in() 避免 @Select foreach 绑定问题 */
    default List<PostMedia> findByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Collections.emptyList();
        return selectList(new LambdaQueryWrapper<PostMedia>()
                .in(PostMedia::getPostId, postIds)
                .orderByAsc(PostMedia::getSortOrder));
    }
}
