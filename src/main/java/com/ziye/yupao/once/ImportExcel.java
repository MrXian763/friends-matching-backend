package com.ziye.yupao.once;

import com.alibaba.excel.EasyExcel;

import java.util.List;

/**
 * 导入 Excel 数据
 */
public class ImportExcel {

    public static void main(String[] args) {
        String fileName = "src/main/resources/testExcel.xlsx";
//        readByLisenter(fileName);
        synchronousRead(fileName);
    }

    /**
     * 监听器读
     * @param fileName
     */
    public static void readByLisenter(String fileName) {
        EasyExcel.read(fileName, XingQiuTableUserInfo.class, new TableListener()).sheet().doRead();
    }

    /**
     * 同步读 数据放在内存中，数据量大不推荐使用
     * @param fileName
     */
    public static void synchronousRead(String fileName) {
        // 这里 需要指定用哪个 class 去读，然后读取第一个 sheet 同步读取会自动 finish
        List<XingQiuTableUserInfo> totalDataList =
                EasyExcel.read(fileName).head(XingQiuTableUserInfo.class).sheet().doReadSync();
        for (XingQiuTableUserInfo xingQiuTableUserInfo : totalDataList) {
            System.out.println(xingQiuTableUserInfo);
        }
    }

}
