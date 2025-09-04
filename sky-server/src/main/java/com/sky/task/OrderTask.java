package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    OrderMapper orderMapper;

    /**
     * 处理订单超时情况
     */
    @Scheduled(cron = "0 * * * * ?")// 每分钟
    public void processTimeoutOrder() {
        // 处理订单超时情况-订单超过15分钟未支付则为超时订单
        List<Orders> ordersList = orderMapper.getByStatus(Orders.PENDING_PAYMENT);// 获取所有状态为待付款的订单
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        if (ordersList != null && ordersList.size() > 0) {// 遍历循环订单列表，查看时间差
            ordersList.forEach(orders -> {
                Long duration = calculateTimeDifference(orders.getOrderTime().toString().replace('T', ' '));
                if (duration >= 15) {
                    // 如果相差时间大于15分钟，则将状态修改为已取消
                    orders.setStatus(Orders.CANCELLED);
                    // 添加取消原因，取消时间
                    orders.setCancelReason("订单支付超时");
                    orders.setCancelTime(LocalDateTime.now());
                    // 更新
                    orderMapper.update(orders);
                }
            });
        }
    }

    /**
     * 处理订单超时情况-黑马写法
     */
   /* @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder2() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            });
        }
    }*/

    /**
     * 计算时间差
     *
     * @param timeString
     * @return
     */
    private static Long calculateTimeDifference(String timeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime parseTime = LocalDateTime.parse(timeString, formatter);
        LocalDateTime currentTime = LocalDateTime.now();

        Duration duration = Duration.between(parseTime, currentTime);
        long seconds = Math.abs(duration.getSeconds());

        Long minutes = seconds / 60;
        log.info("一共相差{}分钟(向下取整)", minutes);

        return minutes;
    }

    /**
     * 处理已完成但是未按确认的派送中的订单
     */
    /*@Scheduled(cron = "0 0 1 * * ?")// 每天凌晨1点
    public void orderComplete() {
        // 获取派送中的订单
        List<Orders> ordersList = orderMapper.getByStatus(Orders.DELIVERY_IN_PROGRESS);
        if (ordersList != null && ordersList.size() > 0) {
            // 全部修改为已完成
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.COMPLETED);
                // 派送到达之后需要指定送达时间-当然，这个的送达时间会晚很多
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            });
        }
    }*/

    /**
     * 处理一直处于派送中的订单-黑马
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单:{}",LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            });
        }
    }
}