package com.ziye.yupao.mapper;

import com.ziye.yupao.model.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author zhuanzhuan
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2024-01-30 18:03:34
* @Entity com.ziye.yupao.model.domain.User
*/
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查找该邮箱的数据记录数
     * @param email 邮箱
     * @return
     */
    Integer countSameEmail(String email);

    /**
     * 根据账号获取用户
     * @param userAccount 账号
     * @return
     */
    User getUserByUserAccount(String userAccount);

    /**
     * 根据用户id更新密码
     * @param id 用户id
     * @param newPassword 新密码
     */
    void updatePasswordById(long id, String newPassword);
}




