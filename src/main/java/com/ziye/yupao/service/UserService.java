package com.ziye.yupao.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziye.yupao.common.BaseResponse;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.dto.ResetPasswordDTO;
import com.ziye.yupao.model.dto.UpdatePasswordDTO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author ziye
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2024-01-30 18:03:34
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param username 用户昵称
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @param email QQ邮箱
     * @return 新用户id
     */
    long userRegister(String username, String userAccount, String userPassword, String checkPassword,
                      String planetCode, String email);

    /**
     * 用户登录
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户数据脱敏
     *
     * @param user 需要脱敏的数据
     * @return 脱敏后的数据
     */
    User getSafetyUser(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    BaseResponse<String> userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 更新用户信息
     * @param user 新数据
     * @param loginUser 旧数据
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户信息
     * @return
     */
    User getLoginUser(HttpServletRequest httpServletRequest);

    /**
     * 鉴权：是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 鉴权：是否为管理员
     *
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 主页获取推荐用户列表
     * @param pageSize 页码
     * @param pageNum 总页书
     * @param request 登录状态
     * @return
     */
    Page<User> getRecommendUsers(long pageSize, long pageNum, HttpServletRequest request);

    /**
     * 匹配用户
     * @param num 匹配的用户数量
     * @param loginUser 当前登录用户
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     * @return
     */
    Page<User> matchUsers(long num,int pageSize, int pageNum, User loginUser);

    /**
     * 用户头像上传
     * @param file 头像
     * @return
     */
    String uploadUserAvatarUrl(MultipartFile file, User loginUser);

    /**
     * 更新用户标签
     * @param tags 新用户标签
     * @param loginUser 当前登录用户
     * @return
     */
    String updateTags(String tags, User loginUser);

    /**
     * 根据id获取用户
     * @param id 用户id
     * @return
     */
    User getUserById(long id);

    /**
     * 修改密码
     * @param updatePasswordDTO 新密码
     * @param loginUser 当前登录用户
     * @return 修改结果
     */
    Boolean updatePassword(UpdatePasswordDTO updatePasswordDTO, User loginUser);

    /**
     * 重置用户密码
     * @param resetPasswordDTO
     * @return 重置结果
     */
    boolean resetPassword(ResetPasswordDTO resetPasswordDTO);
}
