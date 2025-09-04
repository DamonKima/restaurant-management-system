package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("根据分类id查询菜品:{}", categoryId);
        //添加缓存策略
        //注：数据库更新时同时更新的改动在admin的DishController中
        //Redis存储格式：dish_categoryId:value
        //1.查看缓存中是否存在菜品数据
        String key = "dish_" + categoryId;
        List<DishVO> dishVOList = (List<DishVO>) redisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (dishVOList != null && dishVOList.size() > 0) {
            //3.存在则返回
            return Result.success(dishVOList);
        }
        //4.不存在，则去数据库取数据;并且将数据存储到Redis中
        dishVOList = dishService.getByCategoryIdWithFlavor(categoryId);
        redisTemplate.opsForValue().set(key, dishVOList);
        return Result.success(dishVOList);
    }
}