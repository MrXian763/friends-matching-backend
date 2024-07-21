package com.ziye.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ziye.yupao.common.BaseResponse;
import com.ziye.yupao.common.DeleteRequest;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.common.ResultUtils;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.model.domain.Team;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.domain.UserTeam;
import com.ziye.yupao.model.dto.TeamQuery;
import com.ziye.yupao.model.request.TeamAddRequest;
import com.ziye.yupao.model.request.TeamJoinRequest;
import com.ziye.yupao.model.request.TeamQuitRequest;
import com.ziye.yupao.model.request.TeamUpdateRequest;
import com.ziye.yupao.model.vo.TeamUserVO;
import com.ziye.yupao.model.vo.UserVO;
import com.ziye.yupao.service.TeamService;
import com.ziye.yupao.service.UserService;
import com.ziye.yupao.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 队伍接口
 *
 * @author zicai
 */
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    /**
     * 添加队伍
     *
     * @param teamAddRequest 队伍信息
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    /**
     * 修改队伍信息
     *
     * @param teamUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,
                                            HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取队伍
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(Long id) {
        if (id <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    /**
     * 搜索队伍
     *
     * @param teamQuery 队伍信息
     * @param request   登录信息
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<Page<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. 查询队伍列表
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
        // 队伍列表 id
        final List<Long> teamIdList = teamList.stream()
                .map(TeamUserVO::getId)
                .collect(Collectors.toList());
        // 2. 判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIdList);
            // 当前用户加入的队伍集合
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 当前登录用户已加入的队伍 id 集合
            Set<Long> hasJoinTeamSet = userTeamList.stream()
                    .map(UserTeam::getTeamId)
                    .collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamSet.contains(team.getId());
                team.setHasJoin(hasJoin); // 已加入队伍
            });
        } catch (Exception e) {
        }

        // 3. 设置队伍已加入人数
        setTeamJoinNum(teamList, teamIdList);

        // 分页
        int total = teamList.size();
        int pageSize = teamQuery.getPageSize();
        int pageNum = teamQuery.getPageNum();
        if (pageNum * pageSize >= total) { // 避免空指针
            teamList = teamList.subList(pageSize * (pageNum - 1), total);
        } else {
            teamList = teamList.subList(pageSize * (pageNum - 1), pageNum * pageSize);
        }
        Page<TeamUserVO> teamListPage = new Page<>();
        teamListPage.setTotal(total);
        teamListPage.setPages(pageNum);
        teamListPage.setSize(pageSize);
        teamListPage.setRecords(teamList);
        teamListPage.setCurrent(pageNum);

        return ResultUtils.success(teamListPage);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest 要加入的队伍
     * @param request         登录信息
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户退出队伍
     *
     * @param teamQuitRequest 队伍信息
     * @param request         登录信息
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 解散队伍
     *
     * @param deleteRequest 要删除的 id
     * @param request       请求信息
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取当前用户创建的的队伍列表
     *
     * @param teamQuery 创建队伍的用户 id
     * @param request   登录信息
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 根据队伍的创建人 id 查询队伍
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);

        // 设置队伍已加入人数
        List<Long> teamIdList = teamList.stream()
                .map(TeamUserVO::getId)
                .collect(Collectors.toList());
        setTeamJoinNum(teamList, teamIdList);

        return ResultUtils.success(teamList);
    }

    /**
     * 获取当前用户创建的已过期的队伍列表
     *
     * @param teamQuery 创建队伍的用户 id
     * @param request   登录信息
     * @return
     */
    @GetMapping("/list/my/create/timeout")
    public BaseResponse<List<TeamUserVO>> listMyTimeoutTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTimeoutTeams(teamQuery, true);

        // 设置队伍已加入人数
        List<Long> teamIdList = teamList.stream()
                .map(TeamUserVO::getId)
                .collect(Collectors.toList());
        setTeamJoinNum(teamList, teamIdList);

        return ResultUtils.success(teamList);
    }

    /**
     * 获取当前用户加入的队伍
     *
     * @param teamQuery 队伍信息
     * @param request   登录信息
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        List<TeamUserVO> teamList = listMyJoinTeams(teamQuery, request, false);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取当前用户加入的已过期队伍
     *
     * @param teamQuery 队伍信息
     * @param request   登录信息
     * @return
     */
    @GetMapping("/list/my/join/timeout")
    public BaseResponse<List<TeamUserVO>> listMyJoinTimeoutTeams(TeamQuery teamQuery, HttpServletRequest request) {
        List<TeamUserVO> teamList = listMyJoinTeams(teamQuery, request, true);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取队伍成员列表
     *
     * @param teamId  队伍id
     * @param request
     * @return
     */
    @GetMapping("/members")
    public BaseResponse<List<UserVO>> listTeamMembers(Long teamId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        if (teamId == null || teamId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍参数有误");
        }
        List<UserVO> teamMembers = teamService.listTeamMembers(teamId);
        return ResultUtils.success(teamMembers);
    }

    /**
     * 查找用户加入的队伍列表
     *
     * @param teamQuery 查询条件
     * @param request
     * @param isTimeout 是否查询过期的队伍
     * @return
     */
    private List<TeamUserVO> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request, boolean isTimeout) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 根据当前登录用户 id 获取当前用户加入的队伍集合
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        if (userTeamList == null || userTeamList.isEmpty()) {
            return new ArrayList<>(); // 用户没有已加入的队伍
        }
        // 获取所有的队伍 id 集合（去重复后）
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        // 根据队伍 id 获取过期队伍
        teamQuery.setIdList(idList);

        List<TeamUserVO> teamList;

        if (isTimeout) { // 查询过期队伍
            teamList = teamService.listTimeoutTeams(teamQuery, true);
        } else { // 查询当前队伍
            teamList = teamService.listTeams(teamQuery, true);
        }

        // 只返回非本用户创建的队伍
        teamList = teamList.stream()
                .filter(team -> team.getUserId() != loginUser.getId())
                .collect(Collectors.toList());

        // 设置队伍已加入人数
        List<Long> teamIdList = teamList.stream()
                .map(TeamUserVO::getId)
                .collect(Collectors.toList()); // 队伍id集合
        setTeamJoinNum(teamList, teamIdList);

        return teamList;
    }

    /**
     * 设置队伍已加入人数
     *
     * @param teamList   要设置的队伍列表
     * @param teamIdList 队伍id列表
     * @return
     */
    private void setTeamJoinNum(List<TeamUserVO> teamList, List<Long> teamIdList) {
        if (!((teamIdList != null && !teamIdList.isEmpty()) && (teamList != null && !teamList.isEmpty()))) {
            return;
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper); // 当前用户创建的队伍集合
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId)); // 根据队伍id分组
        teamList.forEach(team -> {
            // 设置每个队伍的人数
            team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size());
        });
    }

}
