package com.sky.controller.user;

import com.sky.controller.admin.ShopController;
import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Slf4j
@Api(tags = "C端-分离接口")
public class CategoryController {
    @Autowired
    CategoryService categoryService;

    /**
     * 条件查询
     *
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("条件查询")
    public Result<List<Category>> list(Integer type) {
        log.info("条件查询：{}", type);
        List<Category> categories = categoryService.list(type);
        return Result.success(categories);
    }
}