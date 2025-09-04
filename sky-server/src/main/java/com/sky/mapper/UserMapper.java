package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 查询数据
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenId(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    // ****************订单支付代码-导入****************
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    // select count(id) from user where create_time &lt; endTime
    // select count(id) from user where create_time &gt; beginTime and create_time &lt; endTime
    /**
     * 用户统计接口，获取截止日期前的总用户量;
     * 用户统计接口，获取日期之间用户的创建量
     *
     * @param map
     * @return
     */
    Integer userCountByMap(Map map);
    
}