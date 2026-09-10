package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.exception.BaseException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO selectSum(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        //按 [begin, end] 闭区间逐天生成日期；begin>end 时结果为空列表，避免原来的死循环
        LocalDate current=begin;
        while (!current.isAfter(end)){
            dateList.add(current);
            current=current.plusDays(1);
        }
        List<Map> list=new ArrayList<>();
        for (LocalDate localDate:dateList){
            Map map=new HashMap<>();
            LocalDateTime beginTime=LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(localDate,LocalTime.MAX);
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status",Orders.COMPLETED);
            list.add(map);
        }
        List<Double> doubleList=orderMapper.selectSum(list);
        for (int i = 0; i < doubleList.size(); i++) {
            Double sum=doubleList.get(i);
            if (sum==null){
                doubleList.set(i,0.0);
            }
        }
        String sums=StringUtils.join(doubleList,",");
        String date= StringUtils.join(dateList,",");
        return TurnoverReportVO.builder()
                .dateList(date)
                .turnoverList(sums)
                .build();

    }

    @Override
    public UserReportVO selectUserSum(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        //按 [begin, end] 闭区间逐天生成日期；begin>end 时结果为空列表，避免原来的死循环
        LocalDate current=begin;
        while (!current.isAfter(end)){
            dateList.add(current);
            current=current.plusDays(1);
        }
        List<Integer> userList=new ArrayList<>();
        List<Integer> userAddList=new ArrayList<>();
        for (LocalDate localDate:dateList){
            Map map=new HashMap<>();
            LocalDateTime endTime=LocalDateTime.of(localDate,LocalTime.MAX);
            map.put("end",endTime);
            userList.add(userMapper.selectUser(map));
            LocalDateTime beginTime=LocalDateTime.of(localDate,LocalTime.MIN);
            map.put("begin",beginTime);
            userAddList.add(userMapper.selectUser(map));
        }
        String str1=StringUtils.join(dateList,",");
        String str2=StringUtils.join(userList,",");
        String str3=StringUtils.join(userAddList,",");
        return UserReportVO.builder()
                .dateList(str1)
                .totalUserList(str2)
                .newUserList(str3)
                .build();
    }

    @Override
    public OrderReportVO selectOrders(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        //按 [begin, end] 闭区间逐天生成日期；begin>end 时结果为空列表，避免原来的死循环
        LocalDate current=begin;
        while (!current.isAfter(end)){
            dateList.add(current);
            current=current.plusDays(1);
        }
        List<Integer> ordersList=new ArrayList<>();
        List<Integer> ordersComList=new ArrayList<>();
        for (LocalDate localDate:dateList){
            Map map=new HashMap<>();
            LocalDateTime endTime=LocalDateTime.of(localDate,LocalTime.MAX);
            LocalDateTime beginTime=LocalDateTime.of(localDate,LocalTime.MIN);
            map.put("begin",beginTime);
            map.put("end",endTime);
            ordersList.add(orderMapper.selectCount(map));
            map.put("status",Orders.COMPLETED);
            ordersComList.add(orderMapper.selectCount(map));
        }
        String str1=StringUtils.join(dateList,",");
        String str2=StringUtils.join(ordersList,",");
        String str3=StringUtils.join(ordersComList,",");
        int sum = ordersList.stream().mapToInt(Integer::intValue).sum();
        int sumComplete=ordersComList.stream().mapToInt(Integer::intValue).sum();
        double a=0.0;
        if(sum!=0){
            a=(double)sumComplete/sum;
        }
        return OrderReportVO.builder()
                .dateList(str1)
                .orderCountList(str2)
                .validOrderCountList(str3)
                .totalOrderCount(sum)
                .validOrderCount(sumComplete)
                .orderCompletionRate(a)
                .build();
    }

    @Override
    public SalesTop10ReportVO selectTop10(LocalDate begin, LocalDate end) {
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);
        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        List<GoodsSalesDTO> goodsSalesDTOList=orderDetailMapper.selectTop10(beginTime,endTime);
        List<String> nameList=goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList=goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String name=StringUtils.join(nameList,",");
        String number=StringUtils.join(numberList,",");
        return SalesTop10ReportVO.builder()
                .nameList(name)
                .numberList(number)
                .build();
    }

    @Override
    public void export(HttpServletResponse httpServletResponse) {
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDateTime beginTime=LocalDateTime.of(beginDate,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(endDate,LocalTime.MAX);
        BusinessDataVO businessDataVO=workspaceService.getBusinessData(beginTime,endTime);
        //创建输入流,从资源文件得到连接excel模版的流
        InputStream in=this.getClass().getClassLoader().getResourceAsStream("templete/运营数据报表模板.xlsx");
        if(in==null){
            log.error("运营数据报表模板不存在：templete/运营数据报表模板.xlsx");
            throw new BaseException(MessageConstant.REPORT_TEMPLATE_NOT_FOUND);
        }
        //try-with-resources 保证模板输入流、工作簿、响应输出流在异常时也能正确关闭
        try (InputStream templateIn=in;
             XSSFWorkbook excel=new XSSFWorkbook(templateIn);
             ServletOutputStream outputStream=httpServletResponse.getOutputStream()){

            //设置响应头，让浏览器识别为 xlsx 文件下载（否则前端拿到的是乱码/无文件名）
            String fileName="运营数据报表_"+beginDate+"_"+endDate+".xlsx";
            String encodedName=URLEncoder.encode(fileName,"UTF-8").replaceAll("\\+","%20");
            httpServletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            httpServletResponse.setHeader("Content-Disposition","attachment; filename="+encodedName+"; filename*=UTF-8''"+encodedName);

            //在内存操作excel
            XSSFSheet sheet=excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间"+beginDate+"至"+endDate);
            setNumericCell(sheet.getRow(3).getCell(2),businessDataVO.getTurnover());
            setNumericCell(sheet.getRow(3).getCell(4),businessDataVO.getOrderCompletionRate());
            setNumericCell(sheet.getRow(3).getCell(6),businessDataVO.getNewUsers());
            setNumericCell(sheet.getRow(4).getCell(2),businessDataVO.getValidOrderCount());
            setNumericCell(sheet.getRow(4).getCell(4),businessDataVO.getUnitPrice());

            for (int i=0;i<30;i++){
                LocalDate date=beginDate.plusDays(i);
                BusinessDataVO businessDataVO1=workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN),LocalDateTime.of(date,LocalTime.MAX));
                sheet.getRow(7+i).getCell(1).setCellValue(date.toString());
                setNumericCell(sheet.getRow(7+i).getCell(2),businessDataVO1.getTurnover());
                setNumericCell(sheet.getRow(7+i).getCell(3),businessDataVO1.getValidOrderCount());
                setNumericCell(sheet.getRow(7+i).getCell(4),businessDataVO1.getOrderCompletionRate());
                setNumericCell(sheet.getRow(7+i).getCell(5),businessDataVO1.getUnitPrice());
                setNumericCell(sheet.getRow(7+i).getCell(6),businessDataVO1.getNewUsers());
            }
            //得到tomcat准备的输出流,将内存的excel存入,后续通过输入流传递给前端用户端
            excel.write(outputStream);
            outputStream.flush();

        }catch (IOException ioException){
            //不能用 printStackTrace 吞掉异常，否则报表导出失败时前端只会拿到一个损坏/空白文件
            log.error("导出运营数据报表失败",ioException);
            throw new BaseException("导出运营数据报表失败");
        }
    }

    /**
     * 写入数值单元格：统计结果可能为 null（如区间内无订单），直接 setCellValue 会因自动拆箱抛 NPE，
     * 这里统一兜底为 0。
     */
    private void setNumericCell(XSSFCell cell, Number value) {
        if (cell == null) {
            return;
        }
        cell.setCellValue(value == null ? 0D : value.doubleValue());
    }
}
