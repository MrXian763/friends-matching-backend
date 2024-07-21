package com.ziye.yupao.service;

import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.domain.UserTag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziye.yupao.model.dto.AddTagsDTO;
import com.ziye.yupao.model.vo.ParentTagVO;
import com.ziye.yupao.model.vo.UserTagVO;

import java.util.List;

/**
 * @author xianziye
 * @description 针对表【user_tag(标签)】的数据库操作Service
 * @createDate 2024-06-05 10:56:43
 */
public interface UserTagService extends IService<UserTag> {

    /**
     * 获取所有标签
     * @return
     */
    List<UserTagVO> getAllTags();

    /**
     * 获取所有父标签名称
     * @return
     */
    List<ParentTagVO> getAllParentTags();

    /**
     * 新增标签
     * @param addTagsDTO 新标签
     * @param loginUser
     * @return
     */
    String addTag(AddTagsDTO addTagsDTO, User loginUser);

}
