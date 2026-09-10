package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO selectSum(LocalDate begin, LocalDate end);

    UserReportVO selectUserSum(LocalDate begin, LocalDate end);

    OrderReportVO selectOrders(LocalDate begin, LocalDate end);

    SalesTop10ReportVO selectTop10(LocalDate begin, LocalDate end);

    void export(HttpServletResponse httpServletResponse);
}
