package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    void insertOrder(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 条件更新订单状态：仅当订单当前状态等于 oldStatus 时才改为 newStatus。
     * 用于取消/拒单前「抢占」状态，保证并发下只有一次请求能成功，避免重复退款。
     *
     * @return 受影响行数，0 表示状态已被其他请求改变
     */
    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("oldStatus") Integer oldStatus,
                            @Param("newStatus") Integer newStatus);

    Page<Orders> selectHisTory(OrdersPageQueryDTO ordersPageQueryDTO);

    Orders selectById(Long id);

    Page<Orders> selectPage(OrdersPageQueryDTO ordersPageQueryDTO);

    Integer selectByStatus(Integer status);


    List<Orders> selectBystatusAndTime(Integer status, LocalDateTime time);

    List<Double> selectSum(List<Map> list);


    Integer selectCount(Map map);

    Double selectSumMap(Map map);
}
