package com.ziye.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.mapper.UserTagMapper;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.domain.UserTag;
import com.ziye.yupao.model.dto.AddTagsDTO;
import com.ziye.yupao.model.vo.ParentTagVO;
import com.ziye.yupao.model.vo.UserTagChildren;
import com.ziye.yupao.model.vo.UserTagVO;
import com.ziye.yupao.service.UserTagService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xianziye
 * @description 针对表【user_tag(标签)】的数据库操作Service实现
 * @createDate 2024-06-05 10:56:43
 */
@Service
public class UserTagServiceImpl extends ServiceImpl<UserTagMapper, UserTag>
        implements UserTagService {

    @Resource
    private UserTagMapper userTagMapper;

    /**
     * 获取所有标签
     *
     * @return
     */
    @Override
    public List<UserTagVO> getAllTags() {
        List<UserTagVO> result = new ArrayList<>();
        List<UserTag> userTags = this.list(); // 所有标签列表

        // 构造父子标签树
        userTags.stream()
                .filter(t -> t.getIsParent() == 1) // 只遍历父标签
                .forEach(item -> {
                    // 父标签
                    UserTagVO userTagVO = new UserTagVO();
                    userTagVO.setText(item.getTagName()); // 父标签名称
                    long parentId = item.getId(); // 父标签id
                    List<UserTagChildren> childrenList = new ArrayList<>(); // 子标签集合

                    userTags.stream()
                            .filter(e -> e.getIsParent() == 0 && e.getParentId() == parentId)
                            .forEach(childrenItem -> {
                                UserTagChildren userTagChildren = new UserTagChildren();
                                userTagChildren.setText(childrenItem.getTagName());
                                userTagChildren.setId(childrenItem.getTagName());
                                childrenList.add(userTagChildren);
                            });

                    userTagVO.setChildren(childrenList);
                    result.add(userTagVO);
                });

        return result;
    }

    /**
     * 获取所有父标签名称
     * @return
     */
    @Override
    public List<ParentTagVO> getAllParentTags() {
        List<UserTag> userTags = this.list(); // 所有标签集合
        // 获取所有父标签名称返回
        return userTags.stream()
                .filter(item -> item.getIsParent() == 1)
                .map(t -> {
                    ParentTagVO parentTagVO = new ParentTagVO();
                    parentTagVO.setText(t.getTagName());
                    parentTagVO.setValue(t.getId());
                    return parentTagVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 新增标签
     * @param addTagsDTO 新标签
     * @param loginUser
     * @return
     */
    @Transactional
    @Override
    public String addTag(AddTagsDTO addTagsDTO, User loginUser) {
        if (addTagsDTO == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验是否重复创建标签
        String newChildrenTagName = addTagsDTO.getTagName().toLowerCase();
        String newParentTagName = null;
        if (addTagsDTO.getParentTagName() != null) {
            newParentTagName = addTagsDTO.getParentTagName().toLowerCase();
        }
        List<UserTagVO> allTags = this.getAllTags(); // 获取所有标签
        for (UserTagVO tag : allTags) {
            if (newParentTagName != null) { // 创建父标签
                // 判断要创建的父标签有没有重复
                if (newParentTagName.equals(tag.getText().toLowerCase())) {
                    throw new BussinessException(ErrorCode.PARAMS_ERROR, "父标签已存在");
                }
            }
            // 判断要创建的子标签有没有重复
            for (UserTagChildren childTag : tag.getChildren()) { // 遍历子标签
                if (newChildrenTagName.equals(childTag.getText().toLowerCase())) {
                    throw new BussinessException(ErrorCode.PARAMS_ERROR, "子标签已存在");
                }
            }
        }

        UserTag userTag = new UserTag();
        userTag.setUserId(loginUser.getId()); // 设置标签创建人
        if (addTagsDTO.getParentTagName() == null) { // 只创建子标签
            userTag.setIsParent(0); // 设置标识为非父标签
            userTag.setTagName(addTagsDTO.getTagName()); // 设置标签名字
            BeanUtils.copyProperties(addTagsDTO, userTag);
            int result = userTagMapper.insert(userTag);
            if (result == 1) {
                return "添加成功";
            }
        }

        // 添加父子标签
        String parentTagName = addTagsDTO.getParentTagName();
        userTag.setTagName(parentTagName); // 设置父标签名
        userTag.setIsParent(1); // 设置标识为父标签
        int result = userTagMapper.insert(userTag); // 添加父标签
        if (result == 1) { // 父标签创建成功
            userTag = new UserTag();
            // 根据父标签名称查询id
            QueryWrapper<UserTag> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("tagName", parentTagName);
            List<UserTag> userTags = this.list(queryWrapper);
            if (userTags == null || userTags.isEmpty()) {
                throw new BussinessException(ErrorCode.SYSTEM_ERROR, "添加父标签失败");
            }
            long parentTagId = userTags.get(0).getId();
            userTag.setIsParent(0);
            userTag.setParentId(parentTagId);
            userTag.setTagName(addTagsDTO.getTagName());
            userTag.setUserId(loginUser.getId());
            int addResult = userTagMapper.insert(userTag);
            if (addResult != 1) {
                throw new BussinessException(ErrorCode.SYSTEM_ERROR, "添加子标签失败");
            }
        }
        return "添加成功";
    }

}




