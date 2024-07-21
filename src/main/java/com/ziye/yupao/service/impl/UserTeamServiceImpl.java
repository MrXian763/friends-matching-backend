package com.ziye.yupao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziye.yupao.model.domain.UserTeam;
import com.ziye.yupao.service.UserTeamService;
import com.ziye.yupao.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author xianziye
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-05-10 21:48:06
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




