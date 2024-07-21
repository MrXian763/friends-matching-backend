package com.ziye.yupao.once;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;


/**
 * 星球 Excel 表格用户列信息
 */
@Data
public class XingQiuTableUserInfo {

    /**
     * id
     */
    @ExcelProperty("成员编号")
    private String planetCode;

    /**
     * 用户昵称
     */
    @ExcelProperty("成员昵称")
    private String username;

}
