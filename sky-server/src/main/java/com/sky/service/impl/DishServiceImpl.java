package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应口味
     *
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //插入dish表，单行插入
        //复制数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //设置状态
        dish.setStatus(0);
        //开始插入
        dishMapper.insert(dish);
        //回显菜品id，后面需要传给dishflavor
        Long dishId = dish.getId();

        //插入dishflavor表，批量插入
        //取数据
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        //判断，非空
        if (dishFlavors != null && dishFlavors.size() > 0) {
            //使用lambda表达式将dish的id传给每一个flavor
            dishFlavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            //可以进行批量插入，所以不需要遍历
            dishFlavorMapper.insertBatch(dishFlavors);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        //启动查询器
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        //开始查询
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        //返回结果
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除-起售中菜品不能删除
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus().equals(StatusConstant.ENABLE)) {
                //当前菜品处于起售中
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能够删除-被套餐关联的菜品不能删除
        //方法1.依照上面根据遍历菜品id查找出关系表项，万一这一个菜品没有找到，就查询下一个id；
        //万一找到了，会返回多条setmeal_dish数据，判断返回的条数来确定是否可以删除
        //方法2.视频做法，传入所有的菜品id，判断一遍所有的关系表项，如果表项的菜品id在传入的菜品id
        //集合里，则计入返回的集合中
        //相比第一个方法，不需要遍历菜品id，去一遍一遍的遍历关系表
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品
        //直接批量删除，使用ids即可
        dishMapper.batchDeleteById(ids);
        //删除菜品关联的口味数据
        dishFlavorMapper.batchDeleteByDishId(ids);
    }

    /**
     * 根据菜品id查询菜品
     *
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        //返回的数据要求dish与dish_flavor合并的数据，所以通过分开查询然后结合在一起

        //根据id查询菜品表
        Dish dish = dishMapper.getById(id);
        //根据菜品id查询口味表
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //数据合并，将查询到的数据整合到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        //！在api测试中，当前请求的dishVO中是不需要使用categoryName的，所以为其赋空值   
        dishVO.setCategoryName("");
        return dishVO;
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //更新菜品基本信息
        //疑问：如果传入的数据不完全，有些必须条件缺失了怎么办 答：使用set修改，如果没有传入，就不修改
        dishMapper.update(dish);

        //更新菜品口味
        //一个菜品对应多个口味，需要将所有相关口味删除，在进行添加
        //先删除当前口味（不管商家是否上传了新的口味，都需要将旧口味删除）
        dishFlavorMapper.deleteByDishId(dishDTO.getId());//不使用之前的批量删除，因为这里只需要删除一个菜品的口味
        //判断用户是否传入了菜品口味，没有的话表示该菜品不添加口味，那么完成了删除操作就可以结束该请求；
        //如果用户传入了菜品口味，那么还需要将新的口味插入
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        if (dishFlavors != null && dishFlavors.size() > 0) {
            //添加菜品口味：需要先给flavors添加dish_id
            dishFlavors.forEach(dishFlavor -> dishFlavor.setDishId(dishDTO.getId()));
            //添加菜品口味：插入
            dishFlavorMapper.insertBatch(dishFlavors);
        }
        //视频也是这么处理的，业务逻辑与技术逻辑虽然不一致，但是结果是一样的
    }

    /**
     * 根据分类id查询菜品
     *
     * @param id
     * @return
     */
    public List<Dish> getByCategoryId(Long id) {
        List<Dish> dishes = dishMapper.getByCategoryId(id);
        return dishes;
    }

    /**
     * 菜品起售、停售
     *
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    public List<DishVO> getByCategoryIdWithFlavor(Long categoryId) {
        //1.获取每一项菜品
        //需要查看status是否为起售中，否则不返回
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        List<DishVO> dishVOList = dishMapper.getByCategoryId2(dish);
        //2.获取菜品的口味，并且插入到VO中
        dishVOList.forEach(dishVO -> dishVO.setFlavors(dishFlavorMapper.getByDishId(dishVO.getId())));
        return dishVOList;
    }


}