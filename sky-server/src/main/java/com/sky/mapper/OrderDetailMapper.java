package com.sky.mapper;

import com.sky.dto.SalesTop10ReportDTO;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入
     *
     * @param list
     */
    void insertBatch(List<OrderDetail> list);


    /**
     * 查询订单明细
     * @param id
     * @return
     */
    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    List<SalesTop10ReportDTO> getTop10(LocalDateTime begin,LocalDateTime end);
}