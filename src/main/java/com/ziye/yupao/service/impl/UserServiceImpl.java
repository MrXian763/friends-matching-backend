package com.ziye.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ziye.yupao.common.BaseResponse;
import com.ziye.yupao.common.DefaultUserProps;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.common.ResultUtils;
import com.ziye.yupao.contant.UserConstant;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.mapper.UserMapper;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.dto.ResetPasswordDTO;
import com.ziye.yupao.model.dto.UpdatePasswordDTO;
import com.ziye.yupao.model.enums.UserOnlineStatusEnum;
import com.ziye.yupao.service.FollowRelationshipService;
import com.ziye.yupao.service.SendMailService;
import com.ziye.yupao.service.UserOnlineStatusService;
import com.ziye.yupao.service.UserService;
import com.ziye.yupao.utils.AlgorithmUtils;
import com.ziye.yupao.utils.AliOssUtil;
import com.ziye.yupao.utils.PasswordGenerator;
import com.ziye.yupao.utils.StringValidator;
import com.ziye.yupao.utils.nickname.NicknameValidator;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author ziye
 * 用户服务实现类
 * createDate  2024-01-30 18:03:34
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserOnlineStatusService userOnlineStatusService;

    @Resource
    private SendMailService sendMailService;

    @Resource
    @Lazy
    private FollowRelationshipService followRelationshipService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AliOssUtil aliOssUtil;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "zicai";


    /**
     * 用户注册
     *
     * @param username      用户昵称
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @param email         QQ邮箱
     * @return 新用户id
     */
    @Override
    public long userRegister(String username, String userAccount, String userPassword,
                             String checkPassword, String planetCode, String email) {
        // 1 校验
        // 1.1 非空校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        // 1.2 用户账户不小于4位
        if (userAccount.length() < 4) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        // 1.4 星球编号不大于5位，并且由数字组成
        String validatorResult = StringValidator.planetCodeValidator(planetCode);
        if (validatorResult != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, validatorResult);
        }
        // 1.5 非法字符校验
        String result = StringValidator.isValid(false, userAccount);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }
        result = StringValidator.isValid(true, userPassword);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }
        result = StringValidator.isValid(true, checkPassword);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }
        // 1.6 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 1.7 校验QQ邮箱(不能重复)
        boolean validQQEmail = StringValidator.isValidQQEmail(email);
        if (!validQQEmail) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "QQ邮箱格式错误");
        }
        Integer sameEmailNums = userMapper.countSameEmail(email);
        if (sameEmailNums >= 1) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "当前邮箱已经被使用");
        }
        // 1.8 账号不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "当前账号已经被注册");
        }
        // 1.9 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "星球编号重复");
        }
        // 1.10 校验用户昵称是否非法
        String validateResult = NicknameValidator.validateNickname(username);
        if (validateResult != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, validateResult);
        }

        // 2 密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        user.setUsername(username);
        user.setEmail(email);

        // 设置用户默认数据
        user.setAvatarUrl(DefaultUserProps.AVATAR_URL);
        user.setProfile(DefaultUserProps.PROFILE);

        int insertResult = userMapper.insert(user);

        if (insertResult != 1) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }

        long userId = user.getId();
        // 4. 新用户默认被管理员关注
        followRelationshipService.setDefaultFans(userId);

        // 返回用户id
        return userId;
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        // 1 校验
        // 1.1 非空校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号密码不能为空");
        }
        // 1.2 用户账户不小于4位
        if (userAccount.length() < 4) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        // 1.3 用户密码不小于8位
        String result = StringValidator.isValid(true, userPassword);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }
        // 1.4 校验账户不能包含特殊字符
        result = StringValidator.isValid(false, userAccount);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }

        // 2 查询用户是否存在
        // 根据盐值获取加密后的密码再与数据库密码进行比对
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }

        // 3 用户数据脱敏
        User safetyUser = getSafetyUser(user);

        // 4 记录用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);

        // 记录在线用户
        stringRedisTemplate.opsForValue()
                .set("user:onlineStatus:" + safetyUser.getId(),
                        UserOnlineStatusEnum.ONLINE.getValue(), Duration.ofDays(3));

        // 更新用户在线状态
        userOnlineStatusService.setUserStatus(user.getId(), 1);

        // 返回脱敏后的用户数据
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public BaseResponse<String> userLogout(HttpServletRequest request) {
        // 更新用户在线状态
        User loginUser = this.getLoginUser(request);
        userOnlineStatusService.setUserStatus(loginUser.getId(), 0);
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

        // 记录用户离线
        stringRedisTemplate.opsForValue()
                .set("user:onlineStatus:" + loginUser.getId(),
                        UserOnlineStatusEnum.OFFLINE.getValue(), Duration.ofDays(3));

        return ResultUtils.success("退出成功");
    }

    /**
     * 用户数据脱敏
     *
     * @param user 需要脱敏的数据
     * @return 脱敏后的数据
     */
    @Override
    public User getSafetyUser(User user) {
        if (user == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setPlanetCode(user.getPlanetCode());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setTags(user.getTags());
        safetyUser.setProfile(user.getProfile());
        return safetyUser;
    }

    /**
     * 根据标签搜索用户（内存过滤）
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            // 参数异常，标签集合为空
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1.先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);

        Gson gson = new Gson(); // Google的序列化库

        // 2.在内存中判断是否包含要求的标签
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)) return false;
            // 将 json 字符串反序列化为 java 对象
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            tempTagNameSet = Optional
                    .ofNullable(tempTagNameSet).orElse(new HashSet<>()); // 如果用户标签为空，则新建一个集合，避免空指针
            for (String tagName : tagNameList) {
                if (tempTagNameSet.contains(tagName)) {
                    return true;
                }
            }
            return false;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 根据标签搜索用户（SQL查询版）
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Deprecated // 注解代表已过时
    private List<User> searchUsersByTagsBySQL(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            // 参数异常，标签集合为空
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 拼接and查询
        // like '%java%' and like '%Python%'
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        // 返回脱敏后的数据
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     *
     * @param user      新数据
     * @param loginUser 旧数据
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getId();
        if (userId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果是管理员，允许更新任意用户 否则只能更新当前（自己的）信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BussinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }

        // 校验用户昵称是否合法
        String username = user.getUsername();
        if (username != null) {
            String validateResult = NicknameValidator.validateNickname(username);
            if (validateResult != null) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, validateResult);
            }
        }
        // 校验用户手机号码是否合法
        String phoneNumber = user.getPhone();
        if (StringUtils.isNotBlank(phoneNumber)) {
            boolean phoneNumberValidator = StringValidator.phoneNumberValidator(phoneNumber);
            if (!phoneNumberValidator) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, "手机号码不合法");
            }
        }

        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BussinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 鉴权：是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可以进行查询、删除
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 主页获取推荐用户列表
     *
     * @param pageSize 页码
     * @param pageNum  总页书
     * @param request  登录状态
     * @return
     */
    @Override
    public Page<User> getRecommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        // 无缓存，查数据库
        User loginUser = this.getLoginUser(request);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("id", loginUser.getId()); // 排除自己
        queryWrapper.orderByDesc("createTime"); // 根据创建时间降序排序
        return this.page(new Page<>(pageNum, pageSize), queryWrapper);
    }

    /**
     * 匹配用户
     *
     * @param num       匹配的用户数量
     * @param loginUser 当前登录用户
     * @param pageNum   当前页码
     * @param pageSize  每页数量
     * @return
     */
    @Override
    public Page<User> matchUsers(long num, int pageSize, int pageNum, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        queryWrapper.ne("id", loginUser.getId()); // 不匹配当前登录用户
        // 获取所有标签不为空的用户的 id、tags 字段
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags(); // 当前登录用户的标签（json格式）
        // 将 String 类型的 json 格式标签转为标签集合
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 排除没有标签的用户和自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按照编辑距离由小到大排序，取出前 num 条数据
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 匹配的用户 id 集合（排序好的）
        List<Long> matchUserIdList = topUserPairList.stream()
                .map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        // 根据 id 集合获取所有用户信息（未排序）
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", matchUserIdList);
        // key对应用户id value对应用户对象集合
        Map<Long, List<User>> safeMatchUserList = this.list(userQueryWrapper).stream()
                .map(user -> this.getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        List<User> lastUserList = new ArrayList<>();
        for (Long userId : matchUserIdList) {
            lastUserList.add(safeMatchUserList.get(userId).get(0));
        }

        // 分页
        int total = lastUserList.size();
        if (pageNum * pageSize >= total) { // 避免空指针
            lastUserList = lastUserList.subList(pageSize * (pageNum - 1), total);
        } else {
            lastUserList = lastUserList.subList(pageSize * (pageNum - 1), pageNum * pageSize);
        }
        Page<User> userListPage = new Page<>();
        userListPage.setTotal(total);
        userListPage.setPages(pageNum);
        userListPage.setSize(pageSize);
        userListPage.setRecords(lastUserList);
        userListPage.setCurrent(pageNum);
        return userListPage;
    }

    /**
     * 用户头像上传
     *
     * @param file 头像
     * @return
     */
    @Override
    public String uploadUserAvatarUrl(MultipartFile file, User loginUser) {
        if (file.isEmpty()) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "未上传头像");
        }
        log.info("文件上传：{}", file);

        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            // 截取原始文件名的后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 构造新文件名称
            String objectName = UUID.randomUUID() + extension;
            // 文件的请求路径
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            User newUser = new User();
            BeanUtils.copyProperties(loginUser, newUser);
            newUser.setAvatarUrl(filePath);
            userMapper.updateById(newUser);
            return "头像上传成功";
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
        }
        return "头像上传失败";
    }

    /**
     * 更新用户标签
     *
     * @param tags      新用户标签
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public String updateTags(String tags, User loginUser) {
        // 判断标签格式
        if (tags == null || !(tags.startsWith("[") && tags.endsWith("]"))) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        User newUser = new User();
        BeanUtils.copyProperties(loginUser, newUser);
        newUser.setTags(tags);

        // 用户最多选择10个标签
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> list = gson.fromJson(newUser.getTags(), listType);
        if (list.size() > 10) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户最多选择10个标签");
        }

        this.updateById(newUser);
        return tags;
    }

    /**
     * 根据id获取用户
     *
     * @param id 用户id
     * @return
     */
    @Override
    public User getUserById(long id) {
        User user = this.getById(id);
        return this.getSafetyUser(user);
    }

    /**
     * 修改密码
     *
     * @param updatePasswordDTO 新密码
     * @param loginUser         当前登录用户
     * @return 修改结果
     */
    @Override
    public Boolean updatePassword(UpdatePasswordDTO updatePasswordDTO, User loginUser) {
        if (updatePasswordDTO == null || loginUser == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        String oldPassword = updatePasswordDTO.getOldPassword();
        String newPassword = updatePasswordDTO.getNewPassword();
        String confirmPassword = updatePasswordDTO.getConfirmPassword();

        // 校验非法字符
        String result = StringValidator.isValid(true, oldPassword);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }
        result = StringValidator.isValid(true, newPassword);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }
        result = StringValidator.isValid(true, confirmPassword);
        if (result != null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, result);
        }

        // 判断旧密码和新密码是否相同
        if (oldPassword.equals(newPassword)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "新密码与旧密码相同");
        }

        // 判断旧密码是否正确
        String encryptOldPassword = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes()); // 加密密码比较
        User user = this.getById(loginUser.getId());
        if (!encryptOldPassword.equals(user.getUserPassword())) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }

        // 判断新密码与确认密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "新密码与确认密码不一致");
        }

        // 执行修改
        String encryptNewPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes()); // 保存加密密码
        user.setUserPassword(encryptNewPassword);
        return this.updateById(user);
    }

    /**
     * 重置用户密码
     *
     * @param resetPasswordDTO
     * @return 重置结果
     */
    @Override
    public boolean resetPassword(ResetPasswordDTO resetPasswordDTO) {

        if (resetPasswordDTO == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = resetPasswordDTO.getUserAccount();
        String email = resetPasswordDTO.getEmail();
        if (StringUtils.isBlank(userAccount) || StringUtils.isBlank(email)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 根据账号获取用户
        User user = userMapper.getUserByUserAccount(userAccount);
        if (user == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号不存在");
        }
        // 校验当前用户是否已经绑定邮箱
        String userEmail = user.getEmail();
        if (StringUtils.isBlank(userEmail)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户未绑定邮箱，请联系管理员修改密码");
        }
        // 校验用户输入的邮箱和绑定的邮箱是否一致
        if (!email.equals(userEmail)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "输入的邮箱与账号绑定的不一致");
        }

        // 生成随机8位数密码
        String newPassword = PasswordGenerator.generateDefaultPassword();

        // 将随机密码发送给用户邮箱
        sendMailService.sendMail(email, newPassword);

        // 将新密码加密设置到数据库
        newPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        userMapper.updatePasswordById(user.getId(), newPassword);

        return true;
    }
}




