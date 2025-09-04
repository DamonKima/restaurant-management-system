package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    public void save(SetmealDTO setmealDTO) {
        //取数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //插入setmeal表
        setmealMapper.insert(setmeal);
        //插入setmealDishes表,需要套餐回显id，并插入后才可以放入表中
        //插入套餐id
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {
        //TODO 如果前端是直接通过url发出请求，没有通过前端的话，则会发生错误，报异常，暂时不处理
        //先查询套餐，在查询套餐菜品关系表，最后合并一起
        //查询套餐表
        SetmealVO setmealVO = setmealMapper.getById(id);
        //查询套餐菜品关系表
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        //合并
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    public void updateWithSetmealDishes(SetmealDTO setmealDTO) {
        //修改套餐需要修改套餐表和套餐菜品关系表
        //1.修改套餐表，使用setmeal对象作为参数进行修改
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        //2.修改套餐菜品关系表，实现思维与菜品口味关系表一样，全部删除后在添加新的项目
        //2.1删除
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        //2.2.1给套餐菜品关系数据中添加当前套餐的id
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealDTO.getId()));
        //2.2.2批量添加新的关系
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐起售停售
     *
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断起售套餐内是否有停售菜品，有停售菜品提示”套餐内包含未起售菜品，无法起售“
        if (status.equals(StatusConstant.ENABLE)) {
            //套餐id - 菜品与套餐关联表：得到所有该套餐id的菜品id - 菜品表：逐个查询是否有停售的，若有一个，就发起异常
            List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);//需判断是否有返回值
            if (setmealDishes != null && setmealDishes.size() > 0) {
                setmealDishes.forEach(setmealDish -> {
                    Dish dish = dishMapper.getById(setmealDish.getDishId());
                    if (StatusConstant.DISABLE.equals(dish.getStatus())) {
                        throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Transactional
    public void batchDeleteByIds(List<Long> ids) {
        //判断是不是起售中的套餐
        ids.forEach(id -> {
            SetmealVO setmealVO = setmealMapper.getById(id);
            if (setmealVO.getStatus().equals(StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        //删除套餐需要删除套餐菜品关系 和 删除套餐
        //批量删除套餐
        setmealMapper.batchDeleteByIds(ids);
        //批量删除套餐菜品关系
        setmealDishMapper.batchDeleteBySetmealIds(ids);

    }

    /**
     * 根据分类id查询套餐
     *
     * @param setmeal
     * @return
     */
    public List<Setmeal> getByCategoryId(Setmeal setmeal) {
        return setmealMapper.getByCategoryId(setmeal);
    }

    /**
     * 根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemBySetmealId(Integer id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}