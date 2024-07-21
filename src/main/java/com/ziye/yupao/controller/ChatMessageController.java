package com.ziye.yupao.controller;

import com.ziye.yupao.common.BaseResponse;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.common.ResultUtils;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.vo.ChatMessagesVO;
import com.ziye.yupao.service.ChatMessagesService;
import com.ziye.yupao.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 私聊消息控制器类
 *
 * @author xianziye
 */
@RestController
@RequestMapping("/chat")
public class ChatMessageController {

    @Resource
    private ChatMessagesService chatMessagesService;

    @Resource
    private UserService userService;

    /**
     * 获取历史聊天记录
     *
     * @param senderId 发送者id
     * @param request
     * @return
     */
    @GetMapping("/history/{senderId}")
    public BaseResponse<List<ChatMessagesVO>> getHistoryMessages(@PathVariable("senderId") long senderId,
                                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        long receiverId = loginUser.getId();
        List<ChatMessagesVO> historyMessages = chatMessagesService.getHistoryMessages(senderId, receiverId);
        return ResultUtils.success(historyMessages);
    }

}
