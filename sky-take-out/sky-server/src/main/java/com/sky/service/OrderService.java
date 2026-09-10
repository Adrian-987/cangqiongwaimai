package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO insertOrder(OrdersSubmitDTO ordersSubmitDTO);
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);


    PageResult selectHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端：根据id查询订单详情（不校验归属）
     */
    OrderVO selectOrderDetail(Long id);

    /**
     * 用户端：根据id查询订单详情，仅能查询当前登录用户自己的订单
     */
    OrderVO selectOrderDetailForUser(Long id);

    void cancel(Long id) throws Exception;

    void insertAgain(Long id);

    PageResult selectPage(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO selectBYStatus();

    void updateConfirm(Long id);

    void updateRejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    void updateCancel(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    void updateDelivey(Long id);

    void updateComplete(Long id);

    void reminder(Long id);
}
