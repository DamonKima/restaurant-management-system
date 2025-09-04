package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 使用菜品Id查看购物车中是否有数据
     *
     * @param shoppingCart
     * @return
     */
    Long get(ShoppingCart shoppingCart);

    /**
     * 插入菜品或者套餐
     *
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * number加1，适用于菜品和套餐
     *
     * @param id
     */
    void incrementNumber(Long id);

    //--------------------------------------------------------------

    /**
     * 动态条件查询
     *
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 商品数量加1、商品数量减1
     *
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     *
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 删除购物车中一个商品
     *
     * @param id
     */
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);

    /**
     * 批量插入
     * @param shoppingCartList
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}