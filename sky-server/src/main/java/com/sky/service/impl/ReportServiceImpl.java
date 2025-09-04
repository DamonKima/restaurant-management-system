package com.sky.service.impl;

import com.sky.dto.SalesTop10ReportDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    WorkspaceService workspaceService;

    /**
     * 营业额统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        ArrayList<LocalDate> dateList = getLocalDates(begin, end);

        // 该列表存放营业额数据
        ArrayList<Double> turnoverList = new ArrayList<>();
        // 查看订单，需要诸天查看，若当前有多条订单，则需要将营业额相加，然后添加到营业额列表中
        dateList.forEach(date -> {
            // 需要每一天都进行遍历，遍历日期为当天00，到第二天00
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.COMPLETED, beginTime, endTime);
            // 黑马写法-在SQL中就计算了所有营业额
//            Map map = new HashMap();
//            map.put("begin", beginTime);
//            map.put("end", endTime);
//            map.put("status", Orders.COMPLETED);
//            Double turnOver = orderMapper.sumByMap(map);
//            turnOver = turnOver == null ? 0.0 : turnOver;
//            turnoverList.add(turnOver);
            // 黑马写法
            if (ordersList != null && ordersList.size() > 0) {
                // 如果当前有订单
                BigDecimal totalAmount = ordersList.stream()
                        .map(Orders::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                turnoverList.add(Double.valueOf(totalAmount.toString()));
            } else turnoverList.add(0.0);
        });

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        // 获取日期列表
        ArrayList<LocalDate> dateList = getLocalDates(begin, end);

        // select count(id) from user where create_time &lt; endTime  
        List<Integer> totalUserList = new ArrayList<>();// 用户总量
        // select count(id) from user where create_time &gt; beginTime and create_time &lt; endTime
        List<Integer> newUserList = new ArrayList<>();// 新增用户量
        // 遍历日期列表，获取新增用户以及总用户列表
        dateList.forEach(date -> {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

//            Map map = new HashMap<>();

//            map.put("end", endTime);// 添加截止时间-因为总量只需要小于终止时间就行
//            Integer totalUser = userMapper.userCountByMap(map);
            Integer totalUser = getUserCount(null, endTime);

//            map.put("begin", beginTime);// 添加起始时间
//            Integer newUser = userMapper.userCountByMap(map);
            Integer newUser = getUserCount(beginTime, endTime);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        });

        // 封装结果数据
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 根据条件统计用户数量
     *
     * @param beginTime
     * @param endTime
     * @return
     */
    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Map map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        return userMapper.userCountByMap(map);
    }

    /**
     * 订单统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        // 获取日期列表
        List<LocalDate> datelist = getLocalDates(begin, end);

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        datelist.forEach(date -> {
            // 日期
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 获取集合
//            Map map = new HashMap();

            // 获取所有订单
//            map.put("begin", beginTime);
//            map.put("end", endTime);
//            Integer totalOrderCount = orderMapper.countByMap(map);
            Integer totalOrderCount = getOrderCount(beginTime, endTime, null);
            orderCountList.add(totalOrderCount);

            // 获取有效订单（需要额外添加状态）
//            map.put("status", Orders.COMPLETED);
//            Integer validOrderCount = orderMapper.countByMap(map);
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            validOrderCountList.add(validOrderCount);
        });

        // 订单总数、有效订单数可以在订单列表获取后计算得到
        int totalOrderCount = orderCountList.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
//        int totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        int validOrderCount = validOrderCountList.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
//        int validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = (totalOrderCount == 0 || validOrderCount == 0) ? 0.0 : validOrderCount * 1.0 / totalOrderCount;
        // 封装返回数据
        return OrderReportVO.builder()
                .dateList(StringUtils.join(datelist, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 查询销量排名top10接口
     *
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        // 限定条件：订单下单时间、订单状态
        // 中间条件：将相同菜品份数相加，
        // 查询结果：菜品名称、菜品份数
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<SalesTop10ReportDTO> salesTop10ReportDTOS = orderDetailMapper.getTop10(beginTime, endTime);
        // 商品名称列表-按照销量的高到低排序
        String names = StringUtils.join(salesTop10ReportDTOS.stream().map(SalesTop10ReportDTO::getName).collect(Collectors.toList()), ",");
        // 销量列表-按照高到低排序
        String numbers = StringUtils.join(salesTop10ReportDTOS.stream().map(SalesTop10ReportDTO::getNumber).collect(Collectors.toList()), ",");
        return SalesTop10ReportVO.builder()
                .nameList(names)
                .numberList(numbers)
                .build();
    }

    /**
     * 导出Excel报表接口
     *
     * @param response
     */
    public void export(HttpServletResponse response) throws IOException {
        // 黑马
        // 1. 查询数据库，获取营业数据--查询最近30天的运营数据
        LocalDate beginTime = LocalDate.now().minusDays(30);
        LocalDate endTime = LocalDate.now().minusDays(1);
        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.businessData(LocalDateTime.of(beginTime, LocalTime.MIN), LocalDateTime.of(endTime, LocalTime.MAX));
        // 2. 通过POI将数据写入到Excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            // 基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            // 获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 填充概览数据
            // 填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + beginTime + "至" + endTime);
            // 获取第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            // 获取第五行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());
            // 填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = beginTime.plusDays(i);
                BusinessDataVO businessData = workspaceService.businessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                // 获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            // 3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
       /* // 模板路径、输出路径
        String templatePath = "F:\\找工作\\hm_苍穹外卖\\资料\\day12\\运营数据报表模板.xlsx";
        String outputPath = "F:\\找工作\\hm_苍穹外卖\\资料\\day12\\1.xlsx";
        // 数据整理
        try (Workbook workbook = WorkbookFactory.create(Files.newInputStream(Paths.get(templatePath)))) {// 加载模板
            // 获取第一个sheet
            Sheet sheet = workbook.getSheetAt(0);
            // 获取近30天的 营业额、有效订单、订单完成率、新增用户数、平均客单价 —— 在工作台业务中可以借鉴
            LocalDate beginTime = LocalDate.now().minusDays(30);
            LocalDate endTime = LocalDate.now();
            // 获取30天的平均数据
            BusinessDataVO businessDataVO = overviewData(beginTime, endTime);
            // 定位单元格并写入数据
            setSheetOverview(sheet, businessDataVO);
            // 循环获取每天数据
            for (int i = 7, j = 1; i < 7 + 30; i++) {
                BusinessDataVO data = overviewData(beginTime, beginTime);
                setSheetDetail(sheet, beginTime, i, j, data);
                // 初始天数加1
                beginTime = beginTime.plusDays(1);
            }
            // 保存为新文件（避免覆盖原模板）
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                String fileName = URLEncoder.encode("运营数据报表.xlsx", "UTF-8").replace("+", "%20");


//                workbook.write(fileOutputStream);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }*/
    }

    private void setSheetOverview(Sheet sheet, BusinessDataVO businessDataVO) {
        writeDataToCell(sheet, 3, 2, businessDataVO.getTurnover());
        writeDataToCell(sheet, 3, 4, businessDataVO.getOrderCompletionRate());
        writeDataToCell(sheet, 3, 6, businessDataVO.getNewUsers());
        writeDataToCell(sheet, 4, 2, businessDataVO.getValidOrderCount());
        writeDataToCell(sheet, 4, 4, businessDataVO.getUnitPrice());
    }

    private void setSheetDetail(Sheet sheet, LocalDate beginTime, int i, int j, BusinessDataVO data) {
        writeDataToCell(sheet, i, j++, beginTime);
        writeDataToCell(sheet, i, j++, data.getTurnover());
        writeDataToCell(sheet, i, j++, data.getValidOrderCount());
        writeDataToCell(sheet, i, j++, data.getOrderCompletionRate());
        writeDataToCell(sheet, i, j++, data.getUnitPrice());
        writeDataToCell(sheet, i, j, data.getNewUsers());
    }

    /**
     * 获取工作台数据
     *
     * @return
     */
    public BusinessDataVO overviewData(LocalDate begin, LocalDate end) {
        // 因为是查询今日的数据，所以查询时使用的时间条件应该为今日
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

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
     * 写出到excel文件
     *
     * @param sheet
     * @param rowIndex
     * @param colIndex
     * @param value
     */
    public void writeDataToCell(Sheet sheet, int rowIndex, int colIndex, Object value) {
        try {
            // 1.获取行或创建行
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            // 2.获取或创建单元格
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            // 3.处理空值
            if (value == null) {
                cell.setCellValue("");
                return;
            }
            // 4.根据数据类型设置单元格值
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else if (value instanceof Date) {
                // 设置日期格式（可选）
                Workbook workbook = sheet.getWorkbook();
                CreationHelper creationHelper = workbook.getCreationHelper();
                CellStyle style = workbook.createCellStyle();
                style.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd"));
                cell.setCellStyle(style);
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else {
                cell.setCellValue(value.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("写入单元格失败：[行=" + rowIndex + "，列=" + colIndex + "]", e);
        }
    }

    /**
     * 根据条件统计订单数量
     *
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }


    /**
     * 获取日期列表
     *
     * @param begin
     * @param end
     * @return
     */
    private ArrayList<LocalDate> getLocalDates(LocalDate begin, LocalDate end) {
        // 当前集合用于存放从begin到end范围内每一天的日期
        ArrayList<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);// 需要先把第一天插入，因为循环是会跳过第一天的
        while (!begin.isEqual(end)) {
            // 日期计算 指定天数的后一天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }

}