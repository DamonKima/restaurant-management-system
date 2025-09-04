package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 判断业务异常：请求的地址簿项目为空、购物车为空
        Long userId = BaseContext.getCurrentId();
        // 查询地址簿
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 地址簿为空，抛出业务异常
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 查询购物车
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            // 购物车为空，抛出业务异常
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        // 因为微信商户号搞不到所以在后台默认 为已经付款状态
        // orders.setStatus(Orders.PENDING_PAYMENT);
        // orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.TO_BE_CONFIRMED);// 待接单状态就是已付款后待接单的状态
        orders.setPayStatus(Orders.PAID);// 待接单且已付款状态
        orders.setCheckoutTime(LocalDateTime.now());// 设置支付时间
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        // 设置为已付款状态
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        // address地址详情、username用户名称 这两个数据需要自己进行封装并且填入，之前没有进行这个步骤，现在补充
        // 获取地址并加工数据
        StringBuilder address = new StringBuilder();
        String provinceName = addressBook.getProvinceName();
        String cityName = addressBook.getCityName();
        String districtName = addressBook.getDistrictName();
        String detail = addressBook.getDetail();
        address.append(provinceName).append(cityName).append(districtName).append(detail);
        // 设置地址
        orders.setAddress(String.valueOf(address));
        // 设置用户名称，注意用户名称和收件人是不一样的，但是这里因为微信版本不同，所以是没有获取用户名称的
        orders.setUserName(userMapper.getById(BaseContext.getCurrentId()).getName());
        // 插入
        orderMapper.insert(orders);

        // 向订单详情表插入多条数据
        List<OrderDetail> list = new ArrayList<>();
        // 遍历购物车商品
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orders.getId());
            BeanUtils.copyProperties(cart, orderDetail);
            list.add(orderDetail);
        }
        // 批量插入
        orderDetailMapper.insertBatch(list);
        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);
        // *****************来单提醒*****************
        // 通过websocket向客户端，也就是商家后端浏览器推送消息，type orderId content
        Map map = new HashMap();
        map.put("type", 1);// 1表示来单提醒，2表示客户催单
        map.put("orderId", orders.getId());// 订单Id 
        map.put("content", "订单号" + orders.getNumber());// 订单号

        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);
        // *****************来单提醒*****************
        // 封装VO返回结果 
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    // ****************订单支付代码-导入****************

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery4User(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 需要返回的数据有两种，订单、订单具体菜品数据，每一条订单都拥有一条或者数条的菜品数据
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        // 订单数据处理
        ArrayList<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            // 具体菜品数据处理。通过遍历每一个订单，获取其中的所有菜品数据
            for (Orders orders : page.getResult()) {
                // 查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                // 整合订单数据以及具体菜品数据
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        OrderVO orderVO = new OrderVO();
        // 查询订单信息
        Orders orders = orderMapper.getById(id);
        // 查询订单明细 
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        // 添加菜品信息的设置
        orderVO.setOrderDishes(getOrderDishesStr(orders));
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 查询订单
        Orders orders = orderMapper.getById(id);

        // 检验订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 订单处于待接单状态下取消，需要进行退款
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 调用微信支付退款接口
            /*weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));*/
            // 支付状态修改为退款
            orders.setPayStatus(Orders.REFUND);// 简化退款为仅设置支付状态
        }
        // 更新订单状态、取消原因、取消时间 
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 实质就是将当前订单明细中的商品重新放到购物车中，即可
        // 取出所有的商品明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 遍历并且将商品转化成购物车项目
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            // 添加创建时间、创建人id
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        // 插入购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        // 添加orderDishes订单菜品信息
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOS = getOrderVOList(page);
        // 返回结果
        return new PageResult(page.getTotal(), orderVOS);
    }

    /**
     * 根据订单信息获取返回订单项目列表，并且添加了菜品信息字符串
     *
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 为判定订单明细为空的情况，因为一般下单之后都会有商品添加进来的
        return page.getResult().stream().map(orders -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            String orderDishesStr = getOrderDishesStr(orders);
            orderVO.setOrderDishes(orderDishesStr);
            return orderVO;
        }).collect(Collectors.toList());
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 加工得到orderDishes信息
        // 首先需要查询订单明细，得到菜品，然后将菜品逐条添加到String中
        // 菜品信息格式为：”菜品名*菜品数量；菜品名*菜品数量；菜品名*菜品数量；“
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishList = orderDetailList.stream()
                .map(orderDetail -> orderDetail.getName() + "*" + orderDetail.getNumber() + "；")
                .collect(Collectors.toList());
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        // 需要编写查询语句，查询出三个状态的订单数量
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 设置订单状态即可
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 只有订单处于待接单状态才可以执行拒单操作
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        // 拒单时，如果用户已经完成了支付，需要为用户退款
        Integer payStatus = ordersDB.getPayStatus();
        if (Objects.equals(payStatus, Orders.PAID)) {
            /*// 用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款:{}", refund);*/
            orders.setPayStatus(Orders.REFUND);// 设置付款状态
        }

        // 此外，拒单需要根据订单id更新订单状态、拒单原因、取消时间
        // 商家拒单需要指定拒单原因
        // 设置订单状态为已取消
        orders.setId(ordersRejectionDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());// 拒单原因不会在后端显示，所以同样添加到取消原因中
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 与拒单的区别：
        // 取消订单可以在任何阶段取消
        // 如果用户已经付款，则需要退款
        // 否则直接设置状态为已取消即可（还有其他属性）
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        Orders orders = new Orders();
        // 查看支付状态
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            // 说明用户已支付，需要退款
            /*String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("退款：{}", refund);*/
            orders.setPayStatus(Orders.REFUND);// 简化退款为仅设置支付状态
        }
        // 设置订单状态、取消原因
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 将订单状态修改为 派送中
        // 只有状态为“待派送” 的订单可以执行派送订单操作
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            // 已结单但是没有在派送中，表示未派送
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(id);
        // 更新订单状态
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 将状态修改成已完成
        // 只有在派送中的订单才可以修改
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);// 订单状态有误
        }
        Orders orders = new Orders();
        orders.setId(id);
        // 更新订单状态
        orders.setStatus(Orders.COMPLETED);
        // 更新订单送达时间
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 用户催单
     *
     * @param id
     */
    public void reminder(Long id) {
        Orders orders = orderMapper.getById(id);
        // *****************用户催单*****************
        // 通过websocket向客户端，也就是商家后端浏览器推送消息，type orderId content
        Map map = new HashMap();
        map.put("type", 2);// 1表示来单提醒，2表示客户催单
        map.put("orderId", id);// 订单Id 
        map.put("content", "订单号" + orders.getNumber());// 订单号

        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);
        // *****************用户催单*****************
    }


}