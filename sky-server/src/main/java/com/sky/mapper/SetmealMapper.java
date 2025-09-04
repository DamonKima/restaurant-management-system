package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     *
     * @param cateGoryId
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long cateGoryId);

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 新增套餐
     *
     * @param setmeal
     * @return
     */
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 根据套餐id查询套餐
     *
     * @param id
     * @return
     */
    @Select("select s.*,c.name as categoryName from " +
            "setmeal s left outer join category c on s.category_id = c.id where s.id = #{id}")
    SetmealVO getById(Long id);
    
    @Select("select * from setmeal where id = #{id}")
    Setmeal getById2(Long id);

    /**
     * 修改套餐
     *
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    void batchDeleteByIds(List<Long> ids);

    /**
     * 根据分类id查询套餐
     *
     * @param categoryId
     * @return
     */
    @Select("select * from setmeal where category_id = #{categoryId} and status = #{status}")
    List<Setmeal> getByCategoryId(Setmeal setmeal);

    /**
     * 根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    @Select("select sd.name,sd.copies,d.image,d.description from setmeal_dish sd left join dish d on d.id = sd.dish_id " +
            "where setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Integer setmealId);

    /**
     * 通过状态查询套餐数量
     * @param status
     * @return
     */
    @Select("select count(id) from setmeal where status = #{status}")
    Integer countByStatus(int status);
}