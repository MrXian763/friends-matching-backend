package com.ziye.yupao.aspect;

import com.ziye.yupao.contant.UserConstant;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.service.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 更新登录态切面类
 *
 * @author zicai
 */
@Aspect
@Component
public class UpdateLoginUserAspect {

    public static HttpServletRequest request = null;

    @Resource
    private UserService userService;

    /**
     * 更新用户登录态
     */
    @After("userControllerMethods()")
    public void updateLoginUser(JoinPoint joinPoint) {
        request = (HttpServletRequest) joinPoint.getArgs()[1];
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser != null) {
            long loginUserId = loginUser.getId();
            User user = userService.getById(loginUserId); // 同步数据，获取数据库中的数据
            User safetyUser = userService.getSafetyUser(user); // 数据脱敏
            // 更新session中的用户数据
            request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);
        }
    }

    /**
     * 切入点表达式方法
     * 拦截所有修改用户数据的方法
     */
    @Pointcut("execution(* com.ziye.yupao.controller.UserController.userLogin(..))" +
            " || execution(* com.ziye.yupao.controller.UserController.updateUser(..))" +
            " || execution(* com.ziye.yupao.controller.UserController.upload(..))" +
            " || execution(* com.ziye.yupao.controller.UserController.updateTags(..))")
    public void userControllerMethods() {}

}
