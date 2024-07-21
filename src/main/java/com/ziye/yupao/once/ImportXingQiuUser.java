package com.ziye.yupao.once;

import com.alibaba.excel.EasyExcel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导入星球用户到数据库
 */
public class ImportXingQiuUser {

    public static void main(String[] args) {
        // todo 自定义路径
        String fileName = "src/main/resources/prodExcel.xlsx";
        // 这里 需要指定用哪个 class 去读，然后读取第一个 sheet 同步读取会自动 finish
        List<XingQiuTableUserInfo> userInfoList =
                EasyExcel.read(fileName).head(XingQiuTableUserInfo.class).sheet().doReadSync();
        System.out.println("总数 = " + userInfoList.size());
        // 星球昵称相同的分为同一组
        Map<String, List<XingQiuTableUserInfo>> listMap =
                userInfoList.stream()
                        .filter(userInfo -> StringUtils.isNoneEmpty(userInfo.getUsername())) // 留下用户昵称非空的数据，避免空指针异常
                        .collect(Collectors.groupingBy(XingQiuTableUserInfo::getUsername)); // 重复的用户昵称根据用户昵称分组
        System.out.println("不重复昵称数 = " + listMap.keySet().size());

        for (Map.Entry<String, List<XingQiuTableUserInfo>> stringListEntry : listMap.entrySet()) {
            if (stringListEntry.getValue().size() > 1) {
                System.out.println("username = " + stringListEntry.getKey());
                System.out.println("1");
            }
        }
    }

}
