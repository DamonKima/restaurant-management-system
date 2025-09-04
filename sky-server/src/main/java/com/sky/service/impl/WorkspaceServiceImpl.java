package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    DishMapper dishMapper;

    /**
     * 查询今日运营数据
     *
     * @return
     */
    public BusinessDataVO businessData(LocalDateTime beginTime, LocalDateTime endTime) {
        // 设置查询条件
        Map<Object, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);

        // 有效订单数、订单完成率、平均客单价

        // 1 获取总订单
        Integer totalOrderCount = orderMapper.countByMap(map);
        // 2 获取有效订单数
        map.put("status", Orders.COMPLETED);
        Integer validOrderCount = orderMapper.countByMap(map);
        // 3 计算订单完成率
        Double orderCompletionRate = (totalOrderCount == 0 || validOrderCount == 0) ? 0.0 : validOrderCount * 1.0 / totalOrderCount;

        // 4 查询营业额
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;

        // 5 计算平均客单价-营业额除以有效订单量
        Double uniPrice = (turnover == 0 || validOrderCount == 0) ? 0.0 : turnover * 1.0 / validOrderCount;

        // 6 获取新增用户
        Integer newUsers = userMapper.userCountByMap(map);

        return BusinessDataVO.builder()
                .newUsers(newUsers)
                .orderCompletionRate(orderCompletionRate)
                .turnover(turnover)
                .unitPrice(uniPrice)
                .validOrderCount(validOrderCount)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getOverviewSetmeals() {
        // 已起售套餐数量
        Integer sold = setmealMapper.countByStatus(StatusConstant.ENABLE);
        // 已停售套餐数量
        Integer discontinued = setmealMapper.countByStatus(StatusConstant.DISABLE);
        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getoverviewDishes() {
        // 已起售套餐数量
        Integer sold = dishMapper.countByStatus(StatusConstant.ENABLE);
        // 已停售套餐数量
        Integer discontinued = dishMapper.countByStatus(StatusConstant.DISABLE);
        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOverviewOrders() {
        Map<Object, Object> map = new HashMap<>();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));

        Integer allOrders = orderMapper.countByMap(map);// 全部订单
        map.put("status", Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.countByMap(map);// 待接单
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(map);// 待派送
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);// 已完成
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);// 已取消

        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .build();
    }
}