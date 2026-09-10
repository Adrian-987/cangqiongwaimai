package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    AddressBookMapper addressBookMapper;

    @Autowired
    ShoppingCartMapper shoppingCartMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    WeChatPayUtil weChatPayUtil;

    @Autowired
    WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO insertOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //判空,防止地址和商品为空
        AddressBook addressBook=addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId= BaseContext.getCurrentId();
        //校验收货地址归属当前登录用户，防止伪造addressBookId使用他人地址下单
        if (!addressBook.getUserId().equals(userId)){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_BELONG);
        }

        //调用百度地图检查配送距离
        checkOutOfRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

        List<ShoppingCart> shoppingCartList=shoppingCartMapper.selectList(userId);
        if(shoppingCartList==null || shoppingCartList.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //添加订单表信息
        Orders orders=new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        //金额以服务端按购物车重新计算为准，防止前端传值被篡改
        BigDecimal amount = shoppingCartList.stream()
                .map(sc -> sc.getAmount().multiply(new BigDecimal(sc.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orders.setAmount(amount);
        orders.setOrderTime(LocalDateTime.now());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orderMapper.insertOrder(orders);
        //封装订单详细,并添加数据库
        List<OrderDetail> orderDetailList=new ArrayList<>();
        for (ShoppingCart shoppingCart:shoppingCartList){
            OrderDetail orderDetail=new OrderDetail();
            BeanUtils.copyProperties(shoppingCart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertOrderDetail(orderDetailList);

        //删除购物车,放回订单信息
        shoppingCartMapper.delectByUserId(userId);
        OrderSubmitVO orderSubmitVO=OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //根据订单号查询订单，金额以数据库为准，并校验订单归属
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (ordersDB == null || !ordersDB.getUserId().equals(userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                ordersDB.getAmount(), //支付金额，单位 元
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
        //订单不存在则报错；已支付则直接返回，保证微信重复回调的幂等性
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            return;
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        Map map=new HashMap<>();
        map.put("type",1);
        map.put("orderId",orders.getId());
        map.put("content","订单号"+outTradeNo);
        String jsonString=JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    @Override
    public PageResult selectHistory(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Long currentId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(currentId);
        Page<Orders> page=orderMapper.selectHisTory(ordersPageQueryDTO);
        List<OrderVO> list=new ArrayList<>();
       if (page!=null&& page.size()>0){
           for (Orders orders:page){
                Long orderId=orders.getId();
                List<OrderDetail> orderDetailList=orderDetailMapper.selectByOrderId(orderId);
                OrderVO orderVO=new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
           }
       }
       return new PageResult(page.getTotal(), list);
    }

    /**
     * 管理端：根据id查询订单详情，不校验归属（后台需要查看任意订单）
     */
    @Override
    public OrderVO selectOrderDetail(Long id) {
        Orders orders=orderMapper.selectById(id);
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return buildOrderVO(orders);
    }

    /**
     * 用户端：根据id查询订单详情，仅能查询当前登录用户自己的订单
     */
    @Override
    public OrderVO selectOrderDetailForUser(Long id) {
        Long userId=BaseContext.getCurrentId();
        Orders orders=orderMapper.selectById(id);
        if(orders==null || !orders.getUserId().equals(userId)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return buildOrderVO(orders);
    }

    /**
     * 组装订单详情VO
     */
    private OrderVO buildOrderVO(Orders orders) {
        List<OrderDetail> orderDetailList=orderDetailMapper.selectByOrderId(orders.getId());
        OrderVO orderVO=new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public void cancel(Long id) throws Exception {
        Long userId=BaseContext.getCurrentId();
        Orders orders=orderMapper.selectById(id);
        //校验订单归属，防止用户取消他人订单
        if(orders==null || !orders.getUserId().equals(userId)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //status 为 null 时直接比较会拆箱 NPE，这里一并拦截
        if(orders.getStatus()==null || orders.getStatus()>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //先「抢占」状态：仅当订单仍处于当前状态时才改为已取消。
        //并发/重复点击时只有一个请求能抢占成功，从根上避免重复发起退款。
        int rows=orderMapper.updateStatusIfMatch(id,orders.getStatus(),Orders.CANCELLED);
        if(rows==0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //抢占成功后再发起退款（微信退款以订单号为退款单号，本身也具备幂等性）
        Orders orders1=new Orders();
        orders1.setId(orders.getId());
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            String refund=weChatPayUtil.refund(
                    orders.getNumber(), //商户订单号
                    orders.getNumber(), //商户退款单号
                    orders.getAmount(),//退款金额，单位 元
                    orders.getAmount());//原订单金额
            log.info("用户取消订单，申请退款：{}",refund);

            //支付状态修改为 退款
            orders1.setPayStatus(Orders.REFUND);
        }
        orders1.setStatus(Orders.CANCELLED);
        orders1.setCancelReason("用户退款");
        orders1.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders1);
    }

    @Override
    public void insertAgain(Long id) {
        Long userId=BaseContext.getCurrentId();
        //校验订单归属，防止拿任意订单id复制他人订单
        Orders orders=orderMapper.selectById(id);
        if(orders==null || !orders.getUserId().equals(userId)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetailList=orderDetailMapper.selectByOrderId(id);
        if (orderDetailList==null || orderDetailList.size()==0){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<ShoppingCart> list=orderDetailList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart=new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart,"id");
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);
            return shoppingCart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertList(list);
    }

    @Override
    public PageResult selectPage(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page=orderMapper.selectPage(ordersPageQueryDTO);
        List<OrderVO> list=getOrderVOList(page);
        return  new PageResult(page.getTotal(), list);
    }

    @Override
    public OrderStatisticsVO selectBYStatus() {
        Integer toBeConfirmed=orderMapper.selectByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed=orderMapper.selectByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress=orderMapper.selectByStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO=OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
        return orderStatisticsVO;
    }

    @Override
    public void updateConfirm(Long id) {
        //只有待接单状态的订单才能接单，防止把已完成订单改回已接单
        Orders ordersDB=orderMapper.selectById(id);
        if(ordersDB==null || !Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders=Orders.builder()
                .status(Orders.CONFIRMED)
                .id(id)
                .build();
        orderMapper.update(orders);
    }

    @Override
    public void updateRejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Long id= ordersRejectionDTO.getId();
        Orders orders=orderMapper.selectById(id);
        if(orders==null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //先「抢占」状态，保证并发下只有一次拒单/退款成功
        int rows=orderMapper.updateStatusIfMatch(id,Orders.TO_BE_CONFIRMED,Orders.CANCELLED);
        if(rows==0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Integer payStatus=orders.getPayStatus();
        Orders orders1=new Orders();
        orders1.setId(orders.getId());
        orders1.setStatus(Orders.CANCELLED);
        orders1.setCancelTime(LocalDateTime.now());
        orders1.setCancelReason(ordersRejectionDTO.getRejectionReason());
        if(Orders.PAID.equals(payStatus)){
            //用户已支付，需要退款（金额以订单实际金额为准）
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    orders.getAmount(),
                    orders.getAmount());
            log.info("拒单申请退款：{}", refund);
            //退款后同步支付状态，避免出现「已退款但支付状态仍为已支付」的脏数据
            orders1.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders1);
    }

    @Override
    public void updateCancel(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Long id= ordersRejectionDTO.getId();
        Orders orders=orderMapper.selectById(id);
        //只有待付款、待接单状态的订单才允许取消
        if(orders==null || !(orders.getStatus().equals(Orders.PENDING_PAYMENT) || orders.getStatus().equals(Orders.TO_BE_CONFIRMED))){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //先「抢占」状态，保证并发下只有一次取消/退款成功
        int rows=orderMapper.updateStatusIfMatch(id,orders.getStatus(),Orders.CANCELLED);
        if(rows==0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Integer payStatus=orders.getPayStatus();
        Orders orders1=new Orders();
        orders1.setId(orders.getId());
        orders1.setStatus(Orders.CANCELLED);
        orders1.setCancelTime(LocalDateTime.now());
        orders1.setCancelReason(ordersRejectionDTO.getRejectionReason());
        if(Orders.PAID.equals(payStatus)){
            //用户已支付，需要退款（金额以订单实际金额为准）
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    orders.getAmount(),
                    orders.getAmount());
            log.info("商家取消订单，申请退款：{}", refund);
            //退款后同步支付状态，避免出现「已退款但支付状态仍为已支付」的脏数据
            orders1.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders1);
    }

    @Override
    public void updateDelivey(Long id) {
        Orders orders=orderMapper.selectById(id);
        if(orders==null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders1=new Orders();
        orders1.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orders1.setId(orders.getId());
        orderMapper.update(orders1);
    }

    @Override
    public void updateComplete(Long id) {
        Orders orders=orderMapper.selectById(id);
        if(orders==null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders1=new Orders();
        orders1.setStatus(Orders.COMPLETED);
        orders1.setId(id);
        orders1.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders1);
    }

    @Override
    public void reminder(Long id) {
        Long userId=BaseContext.getCurrentId();
        Orders orders=orderMapper.selectById(id);
        //校验订单归属，防止用户催单他人订单
        if(orders==null || !orders.getUserId().equals(userId)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map=new HashMap<>();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号"+orders.getNumber());
        String string=JSON.toJSONString(map);
        webSocketServer.sendToAllClient(string);
    }

    private List<OrderVO> getOrderVOList(List<Orders> page) {
        List<OrderVO> list=page.stream().map(orders -> {
            OrderVO orderVO=new OrderVO();
            BeanUtils.copyProperties(orders,orderVO);
            String dishes=getDishes(orders.getId());
            orderVO.setOrderDishes(dishes);
            return orderVO;
        }).collect(Collectors.toList());
        return list;
    }

    private String getDishes(Long id) {
        List<OrderDetail> list=orderDetailMapper.selectByOrderId(id);
        List<String> StringList=list.stream().map(orderDetail -> {
            String orderDish = orderDetail.getName() + "*" + orderDetail.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        return String.join("",StringList);
    }
    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(jsonObject == null || !"0".equals(jsonObject.getString("status"))){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(jsonObject == null || !"0".equals(jsonObject.getString("status"))){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        //无可用路线时直接报错，避免 jsonArray.get(0) 越界
        if(jsonArray == null || jsonArray.isEmpty()){
            throw new OrderBusinessException("配送路线规划失败");
        }
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
