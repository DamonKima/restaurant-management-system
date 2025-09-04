package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 判断当前加入购物车的商品是否已经存在
        // 怎么知道已经有数据呢：看菜品和套餐ID哪个是非空的，就知道本次传进来添加到购物车的是哪一个了
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long id = shoppingCartMapper.get(shoppingCart);
        if (shoppingCartDTO.getDishId() != null) {// 我这里先判断传进来的是菜品还是套餐，然后在处理，黑马则是先判断数据是否存在
            // 传进来的是菜品，使用用户Id、菜品Id、菜品口味去查询是否已经存在购物车中
            if (id == null) {
                // 菜品不在购物车中，则直接插入新的菜品数据
                Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());// 先取出菜品数据，后面需要使用
                BeanUtils.copyProperties(dish, shoppingCart);// ！！！对于shoppingcart中的id是否会与dishid一样，这里有点奇怪，实测不会出现这种情况。即使传入了数值，也会自动生成id覆盖掉
                shoppingCart.setNumber(1);
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setCreateTime(LocalDateTime.now());// 因为前面dish复制的时候会将创建时间也复制进去，所以需要重新赋值
                shoppingCartMapper.insert(shoppingCart);
                return;
            }
        } else {
            // 传进来的是套餐，使用套餐ID去查询是否已经存在购物车中
            if (id == null) {
                // 说明套餐先前并没有添加到购物车中
                Setmeal setmeal = setmealMapper.getById2(shoppingCartDTO.getSetmealId());
                BeanUtils.copyProperties(setmeal, shoppingCart);
                shoppingCart.setNumber(1);
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCartMapper.insert(shoppingCart);
                return;
            }
        }
        // 当传进来的商品存在时，将其数量加1即可
        shoppingCartMapper.incrementNumber(id);
        // --------------黑马写法
        /*// 判断当前加入到购物车的商品是否已经存在
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);// 注意，这里已经把菜品id、菜品口味、套餐id都放到商品项中了
        shoppingCart.setUserId(userId);// 这里把用户id也放到商品项中，因此后面不需要在放入这些值
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        // 如果已经存在，则只需要将数量加1
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            // 如果不存在，需要插入一条购物车数据
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                // 本次添加到购物车的商品是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            } else {
                // 本次添加到购物车的商品是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById2(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }*/
    }

    /**
     * 查看购物车
     *
     * @return
     */
    public List<ShoppingCart> list() {
        return shoppingCartMapper.list(ShoppingCart.builder().userId(BaseContext.getCurrentId()).build());
    }

    /**
     * 清空购物车
     */
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 删除购物车中一个商品
     *
     * @param shoppingCartDTO
     */
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 查出该商品的数据项
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            if(cart.getNumber() > 1){
                // 如果商品number大于1，则不用从数据库中删除，更新即可
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(cart);
            }else {
                // 否则需要将该数据从数据库中删除
                shoppingCartMapper.deleteById(cart.getId());
            }
        }
    }
}