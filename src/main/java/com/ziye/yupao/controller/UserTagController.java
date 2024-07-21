package com.ziye.yupao.controller;

import com.ziye.yupao.common.BaseResponse;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.common.ResultUtils;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.dto.AddTagsDTO;
import com.ziye.yupao.model.vo.ParentTagVO;
import com.ziye.yupao.model.vo.UserTagVO;
import com.ziye.yupao.service.UserService;
import com.ziye.yupao.service.UserTagService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户标签接口
 *
 * @author zicai
 */
@RestController
@RequestMapping("/tag")
public class UserTagController {

    @Resource
    private UserService userService;

    @Resource
    private UserTagService userTagService;

    /**
     * 获取所有标签
     *
     * @return
     */
    @GetMapping("/all")
    public BaseResponse<List<UserTagVO>> getAllTags(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        List<UserTagVO> tags = userTagService.getAllTags();
        return ResultUtils.success(tags);
    }

    /**
     * 获取所有父标签名称
     *
     * @param request
     * @return
     */
    @GetMapping("/all/parent")
    public BaseResponse<List<ParentTagVO>> getAllParentTags(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        List<ParentTagVO> parentTags = userTagService.getAllParentTags();
        return ResultUtils.success(parentTags);
    }

    /**
     * 用户新增标签
     *
     * @param addTagsDTO 新标签
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<String> addTag(@RequestBody AddTagsDTO addTagsDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        String result = userTagService.addTag(addTagsDTO, loginUser);
        return ResultUtils.success(result);
    }

}
