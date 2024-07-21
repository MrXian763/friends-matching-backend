package com.ziye.yupao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziye.yupao.model.domain.Tag;
import com.ziye.yupao.mapper.TagMapper;
import com.ziye.yupao.service.TagService;
import org.springframework.stereotype.Service;

/**
* @author xianziye
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2024-04-16 19:47:25
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




