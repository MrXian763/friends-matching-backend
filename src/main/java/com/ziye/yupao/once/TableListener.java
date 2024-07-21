package com.ziye.yupao.once;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 读取 Excel 表格数据监听器
 */
@Slf4j
public class TableListener implements ReadListener<XingQiuTableUserInfo> {

    /**
     * 每一条数据解析都会来调用
     *
     * @param data
     * @param context
     */
    @Override
    public void invoke(XingQiuTableUserInfo data, AnalysisContext context) {
        // 输出读到的每条数据
        System.out.println(data);
    }

    /**
     * 解析完所有数据调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        System.out.println("已解析完成");
    }

}