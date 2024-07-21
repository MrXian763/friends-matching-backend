package com.ziye.yupao.service;

import com.ziye.yupao.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.dto.TeamQuery;
import com.ziye.yupao.model.request.TeamJoinRequest;
import com.ziye.yupao.model.request.TeamQuitRequest;
import com.ziye.yupao.model.request.TeamUpdateRequest;
import com.ziye.yupao.model.vo.TeamUserVO;
import com.ziye.yupao.model.vo.UserVO;

import java.util.List;

/**
* @author xianziye
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-05-10 21:47:03
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     * @param team 队伍
     * @param loginUser 登录用户
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     * @param teamQuery 队伍信息
     * @param isAdmin 是否为管理员
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, Boolean isAdmin);

    /**
     * 搜索当前用户创建的过期队伍
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTimeoutTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 修改队伍信息
     * @param teamUpdateRequest 队伍信息
     * @param loginUser 当前登录用户
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 用户加入队伍
     * @param teamJoinRequest 要加入的队伍
     * @param loginUser 当前登录用户
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 用户退出队伍
     * @param teamQuitRequest 队伍信息
     * @param loginUser 当前登录用户
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 解散队伍
     * @param teamId 队伍 id
     * @param loginUser 当前登录用户
     * @return
     */
    boolean deleteTeam(long teamId, User loginUser);

    /**
     * 获取队伍成员
     * @param teamId 队伍id
     * @return
     */
    List<UserVO> listTeamMembers(Long teamId);
}
