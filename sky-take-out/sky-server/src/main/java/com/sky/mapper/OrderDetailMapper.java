package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderDetailMapper {

    void insertOrderDetail(List<OrderDetail> orderDetailList);


    List<OrderDetail> selectByOrderId(Long orderId);

    List<GoodsSalesDTO> selectTop10(LocalDateTime beginTime, LocalDateTime endTime);
}
