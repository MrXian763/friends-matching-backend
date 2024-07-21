package com.ziye.yupao.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.mapper.TeamMapper;
import com.ziye.yupao.model.domain.Team;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.domain.UserTeam;
import com.ziye.yupao.model.dto.TeamQuery;
import com.ziye.yupao.model.enums.TeamStatusEnum;
import com.ziye.yupao.model.request.TeamJoinRequest;
import com.ziye.yupao.model.request.TeamQuitRequest;
import com.ziye.yupao.model.request.TeamUpdateRequest;
import com.ziye.yupao.model.vo.TeamUserVO;
import com.ziye.yupao.model.vo.UserVO;
import com.ziye.yupao.service.TeamService;
import com.ziye.yupao.service.UserService;
import com.ziye.yupao.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xianziye
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-05-10 21:47:03
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 创建队伍
     *
     * @param team      队伍
     * @param loginUser 登录用户
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参数不能为空
        if (team == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 校验是否登录
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        // 3. 校验信息
        // 3.1 队伍人数 > 1 并且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        // 3.2 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        // 3.3 队伍描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) && description.length() > 512) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        // 3.4 status 是否公开，不公开默认为0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        // 3.5 如果 status 是加密状态，必须要有密码，密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        // 3.6 超时时间不能小于当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "超时时间非法");
        }
        // 3.7 校验用户最多创建 5 个队伍，排出超时的队伍
        // TODO 有 bug 用户快速点击可以创建超出范围队伍数量 （加锁）
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.gt("expireTime", new Date());
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "最多创建 5 个队伍");
        }
        // 4. 插入队伍信息到队伍中
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 5. 插入用户 => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return 0;
    }

    /**
     * 搜索队伍
     *
     * @param teamQuery 队伍信息
     * @param isAdmin   是否为管理员
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, Boolean isAdmin) {
        return listTeams(teamQuery, isAdmin, false);
    }

    /**
     * 搜索当前用户创建的过期队伍
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    @Override
    public List<TeamUserVO> listTimeoutTeams(TeamQuery teamQuery, boolean isAdmin) {
        return listTeams(teamQuery, isAdmin, true);
    }

    /**
     * 修改队伍信息
     *
     * @param teamUpdateRequest 队伍信息
     * @param loginUser         当前登录用户
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        Team oldTeam = this.getTeamById(id);

        // 只有管理员或者队伍创建者可以修改
        if ((oldTeam.getUserId() != loginUser.getId()) && (!userService.isAdmin(loginUser))) {
            throw new BussinessException(ErrorCode.NO_AUTH);
        }
        // 将状态设置为加密需要设置密码
        TeamStatusEnum newStatusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus()); // 要修改的状态
        TeamStatusEnum oldStatusEnum = TeamStatusEnum.getEnumByValue(oldTeam.getStatus()); // 修改之前的状态
        if (!oldStatusEnum.equals(TeamStatusEnum.SECRET)) {
            if (newStatusEnum.equals(TeamStatusEnum.SECRET)) {
                if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                    throw new BussinessException(ErrorCode.PARAMS_ERROR, "加密房间必须设置密码");
                }
            }
        }
        // 修改的超时时间不能小于当前时间
        Date expireTime = teamUpdateRequest.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "超时时间非法");
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest 要加入的队伍
     * @param loginUser       当前登录用户
     * @return
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 要加入的队伍必须存在
        Long teamId = teamJoinRequest.getTeamId();
        Team team = this.getTeamById(teamId);
        // 不能加入超时队伍
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        // 不能加入私有队伍
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        // 加入加密房间需要匹配密码
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }

        // 分布式锁
        RLock lock = redissonClient.getLock("zicai:join_team");
        // 等待时间 释放时间 单位
        try {
            while (true) {
                // 只有一个线程能获取到锁
                if (lock.tryLock(0, 30000L, TimeUnit.MILLISECONDS)) {
                    // 用户最多创建和加入 5 个队伍
                    long userId = loginUser.getId();
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);

                    // 用户加入的队伍列表（包括过期）
                    List<UserTeam> hasJoinUserTeamList = userTeamService.list(userTeamQueryWrapper);
                    // 用户加入的队伍id列表（包括过期）
                    List<Long> hasJoinTeamIdList = hasJoinUserTeamList.stream()
                            .map(UserTeam::getTeamId)
                            .collect(Collectors.toList());

                    // 获取用户加入的队伍集合
                    if (hasJoinTeamIdList.isEmpty()) {
                        // 队伍人数是否已满
                        long teamHasJoinNum = this.countTeamUserByTeamId(teamId); // 统计当前队伍人数
                        if (teamHasJoinNum >= team.getMaxNum()) {
                            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍已满人");
                        }
                        // 修改队伍信息
                        UserTeam userTeam = new UserTeam();
                        userTeam.setUserId(userId);
                        userTeam.setTeamId(teamId);
                        userTeam.setJoinTime(new Date());
                        return userTeamService.save(userTeam);
                    }

                    QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
                    teamQueryWrapper.in("id", hasJoinTeamIdList);
                    List<Team> hasJoinTeamList = this.list(teamQueryWrapper);

                    List<Team> teams = hasJoinTeamList.stream()
                            .filter(t -> t.getExpireTime().after(new Date()))
                            .collect(Collectors.toList());

                    long hasJoinNum = teams.size();
                    if (hasJoinNum >= 5) {
                        throw new BussinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    // 不能重复加入队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BussinessException(ErrorCode.PARAMS_ERROR, "不能重复加入队伍");
                    }
                    // 队伍人数是否已满
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId); // 统计当前队伍人数
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍已满人");
                    }
                    // 修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doJoinTeam error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) { // 判断当前这个锁是不是这个线程的
                lock.unlock(); // 释放锁
            }
        }
    }

    /**
     * 用户退出队伍
     *
     * @param teamQuitRequest 队伍信息
     * @param loginUser       当前登录用户
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = this.getTeamById(teamId);
        // 判断用户是否已经加入队伍
        long userId = loginUser.getId();
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(userTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum == 1) { // 队伍只有一人，解散队伍
            // 删除队伍
            this.removeById(teamId);
        } else { // 队伍中还剩至少两人
            if (team.getUserId() == userId) { // 当前登录用户是否为队长
                // 把队长转移给最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BussinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                // 更新队伍队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId); // 要更新的队伍 id
                updateTeam.setUserId(nextTeamLeaderId); // 队伍队长 id
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BussinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 删除 用户-队伍 关联关系
        return userTeamService.remove(queryWrapper);
    }

    /**
     * 解散队伍
     *
     * @param teamId    队伍 id
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long teamId, User loginUser) {
        // 校验队伍是否存在
        Team team = getTeamById(teamId);
        // 校验当前登录用户是不是队伍的队长
        if (team.getUserId() != loginUser.getId()) {
            throw new BussinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        return this.removeById(teamId);
    }

    /**
     * 根据 id 获取队伍信息
     *
     * @param teamId 队伍 id
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 根据队伍 id 查询队伍人数
     *
     * @param teamId 队伍 id
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        return userTeamService.count(queryWrapper);
    }

    /**
     * 查询队伍
     * @param teamQuery 查询条件
     * @param isAdmin 是否管理员
     * @param isTimeOut 是否查询过期队伍
     * @return
     */
    private List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin, boolean isTimeOut) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            // 根据关键词匹配队伍名称和描述
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            // 根据名称查询
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            // 根据描述查询
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            // 根据最大人数查询
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum != null) { // 没设置状态默认查询所有队伍
                if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                    throw new BussinessException(ErrorCode.NO_AUTH);
                }
                queryWrapper.eq("status", statusEnum.getValue());
            }
        }

        // 是否展示已过期队伍
        if (isTimeOut) {
            queryWrapper.and(qw -> qw.lt("expireTime", new Date()).or().isNull("expireTime"));
        } else {
            queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        }
        queryWrapper.orderByDesc("createTime"); // 根据创建时间降序排序
        List<Team> teamList = this.list(queryWrapper); // 查询出来的队伍集合
        if (CollectionUtils.isEmpty(teamList)) { // 查不到数据则返回空集合
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 关联查询创建人信息
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) continue;
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
                teamUserVOList.add(teamUserVO);
            }
        }
        return teamUserVOList;
    }

    /**
     * 获取队伍成员
     * @param teamId 队伍id
     * @return
     */
    @Override
    public List<UserVO> listTeamMembers(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍参数有误");
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);// 查询用户队伍关联表
        // 获取队伍成员id集合
        List<Long> memberIds = userTeamList.stream()
                .map(UserTeam::getUserId)
                .collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", memberIds);
        List<User> memberList = userService.list(userQueryWrapper);
        List<UserVO> memberVOList = new ArrayList<>();
        memberList.forEach(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            memberVOList.add(userVO);
        });
        return memberVOList;
    }

}




