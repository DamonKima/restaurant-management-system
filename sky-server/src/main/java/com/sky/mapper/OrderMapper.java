package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     *
     * @param orders
     */
    void insert(Orders orders);


    // ****************订单支付代码-导入****************

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);

    /**
     * 历史订单查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 通过id查询订单
     *
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 通过状态码获取对应状态的订单数
     *
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    int countStatus(Integer status);

    /**
     * 通过状态获取订单
     *
     * @param pendingPayment
     * @return
     */
    @Select("select * from orders where status = #{pendingPayment}")
    List<Orders> getByStatus(Integer pendingPayment);

    /**
     * 根据订单状态和下单时间查询订单
     *
     * @param pendingPayment
     * @param orderTime
     */
    @Select("select * from orders where status = #{pendingPayment} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer pendingPayment, LocalDateTime orderTime);

    @Select("select * from orders where status = #{status} and order_time >= #{beginTime} and order_time <= #{endTime} order by order_time")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 黑马写法-根据动态条件统计营业额数据
     *
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 通过时间，状态获取订单数
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}