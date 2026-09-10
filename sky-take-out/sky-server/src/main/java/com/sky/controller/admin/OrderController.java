package com.sky.controller.admin;

import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    OrderService orderService;

    @GetMapping("/conditionSearch")
    public Result<PageResult> selectPAge(OrdersPageQueryDTO ordersPageQueryDTO){
        PageResult pageResult=orderService.selectPage(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> selectStatistics(){
        OrderStatisticsVO orderStatisticsVO=orderService.selectBYStatus();
        return Result.success(orderStatisticsVO);
    }

    @GetMapping("/details/{id}")
    public Result<OrderVO> selectById(@PathVariable Long id){
        OrderVO orderVO=orderService.selectOrderDetail(id);
        return Result.success(orderVO);
    }

    @PutMapping("/confirm")
    public Result updateConfirm(@RequestBody OrdersDTO ordersDTO){
        orderService.updateConfirm(ordersDTO.getId());
        return Result.success();
    }

    @PutMapping("/rejection")
    public Result updateRejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        orderService.updateRejection(ordersRejectionDTO);
        return Result.success();
    }


    @PutMapping("/cancel")
    public Result updateCancel(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        orderService.updateCancel(ordersRejectionDTO);
        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    public Result updateDelivey(@PathVariable Long id) {
        orderService.updateDelivey(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    public Result updateComplete(@PathVariable Long id){
        orderService.updateComplete(id);
        return Result.success();
    }
}
