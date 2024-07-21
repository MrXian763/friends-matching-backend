package com.ziye.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ziye.yupao.common.BaseResponse;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.common.ResultUtils;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.dto.ResetPasswordDTO;
import com.ziye.yupao.model.dto.UpdatePasswordDTO;
import com.ziye.yupao.model.dto.UserTagsDTO;
import com.ziye.yupao.model.request.UserLoginRequest;
import com.ziye.yupao.model.request.UserRegisterRequest;
import com.ziye.yupao.service.FollowRelationshipService;
import com.ziye.yupao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.ziye.yupao.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 *
 * @author zicai
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private FollowRelationshipService followRelationshipService;

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求体（用户名、密码）
     * @param request
     * @return 用户数据
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号密码不能为空");
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求体（账户、密码、校验密码）
     * @return 注册的用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "参数非法");
        }
        String username = userRegisterRequest.getUsername();
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        String email = userRegisterRequest.getEmail();
        if (StringUtils.isAnyBlank(username, userAccount, userPassword, checkPassword, planetCode, email)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        long userId = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        return ResultUtils.success(userId);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<String> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        return userService.userLogout(request);
    }

    /**
     * 查询用户数据
     *
     * @param username 用户名
     * @param request
     * @return 查询到的用户集合
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        // 鉴权，管理员才可以查询
        if (!userService.isAdmin(request)) {
            throw new BussinessException(ErrorCode.NO_AUTH, "管理员才可查询");
        }
        // 根据用户名查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        // 返回脱敏数据
        List<User> list = userList.stream()
                .map(user -> userService.getSafetyUser(user))
                .collect(Collectors.toList());
        return ResultUtils.success(list);

    }

    /**
     * 主页用户推荐展示
     *
     * @param pageSize 当前页码
     * @param pageNum  页号
     * @param request  登录状态
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        Page<User> recommendUsers = userService.getRecommendUsers(pageSize, pageNum, request);
        return ResultUtils.success(recommendUsers);
    }

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 标签集合
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 删除用户
     *
     * @param id      用户id
     * @param request
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        // 鉴权：管理员才可以删除
        if (!userService.isAdmin(request)) {
            throw new BussinessException(ErrorCode.NO_AUTH, "管理员才可删除");
        }
        if (id <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "id不能为负");
        }
        boolean removeResult = userService.removeById(id);
        return ResultUtils.success(removeResult);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return 当前登录用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        // 获取当前用户登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        // 返回脱敏后的用户数据
        long userId = currentUser.getId();
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 更新用户数据
     *
     * @param user    新数据
     * @param request 用户登录信息
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        // 校验参数是否为空
        if (user == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户信息（旧信息）
        User loginUser = userService.getLoginUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据标签匹配用户
     *
     * @param num     匹配的用户数量
     * @param request 登录信息
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<Page<User>> matchUsers(long num, int pageSize, int pageNum, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "最多匹配20个用户");
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num, pageSize, pageNum, loginUser));
    }

    /**
     * 用户头像上传
     *
     * @param file 头像
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String result = userService.uploadUserAvatarUrl(file, loginUser);

        return ResultUtils.success(result);
    }

    /**
     * 用户修改标签
     *
     * @param userTagsDTO 新标签数据
     * @param request
     * @return
     */
    @PostMapping("/update/tags")
    public BaseResponse<String> updateTags(@RequestBody UserTagsDTO userTagsDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        String tags = userTagsDTO.getTags();
        String newTags = userService.updateTags(tags, loginUser);
        return ResultUtils.success(newTags);
    }

    /**
     * 关注用户
     *
     * @param id 要关注的用户id
     * @return
     */
    @PostMapping("/dofollow/{id}")
    public BaseResponse<String> followUser(@PathVariable(value = "id") long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        String followResult = followRelationshipService.followUser(id, loginUser);
        return ResultUtils.success(followResult);
    }

    /**
     * 取消关注用户
     *
     * @param id 要取消关注的用户id
     * @return
     */
    @PostMapping("/unfollow/{id}")
    public BaseResponse<String> unFollowUser(@PathVariable(value = "id") long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        String followResult = followRelationshipService.unFollowUser(id, loginUser);
        return ResultUtils.success(followResult);
    }

    /**
     * 获取用户的粉丝数量
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/fans/{id}")
    public BaseResponse<Long> getFansNum(@PathVariable(value = "id") long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        long fansNum = followRelationshipService.getFansNum(id);
        return ResultUtils.success(fansNum);
    }

    /**
     * 获取用户的关注数量
     *
     * @param id      用户id
     * @param request
     * @return
     */
    @GetMapping("/follows/{id}")
    public BaseResponse<Long> getFollowNum(@PathVariable(value = "id") long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        long followNum = followRelationshipService.getFollowNum(id);
        return ResultUtils.success(followNum);
    }

    /**
     * 判断当前登录用户是否已关注用户
     *
     * @param id      被关注用户id
     * @param request
     * @return
     */
    @GetMapping("/isfans/{id}")
    public BaseResponse<Boolean> isFans(@PathVariable(value = "id") long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        boolean isFans = followRelationshipService.isFans(id, loginUser);
        return ResultUtils.success(isFans);
    }

    /**
     * 根据id获取用户
     *
     * @param id 用户id
     * @return
     */
    @GetMapping("/{id}")
    public BaseResponse<User> getUserById(@PathVariable("id") long id) {
        User user = userService.getUserById(id);
        return ResultUtils.success(user);
    }

    /**
     * 更新密码
     *
     * @param updatePasswordDTO 新密码
     * @param request           当前登录用户信息
     * @return 修改结果
     */
    @PostMapping("/password")
    public BaseResponse<Boolean> updatePassword(@RequestBody UpdatePasswordDTO updatePasswordDTO,
                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        Boolean updateResult = userService.updatePassword(updatePasswordDTO, loginUser);
        return ResultUtils.success(updateResult);
    }

    /**
     * 重置密码
     *
     * @param resetPasswordDTO
     * @return
     */
    @PostMapping("/password/reset")
    public BaseResponse<Boolean> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        if (resetPasswordDTO == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean resetResult = userService.resetPassword(resetPasswordDTO);
        return ResultUtils.success(resetResult);
    }

}
