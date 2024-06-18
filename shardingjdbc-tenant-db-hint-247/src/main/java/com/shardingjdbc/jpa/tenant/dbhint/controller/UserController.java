package com.shardingjdbc.jpa.tenant.dbhint.controller;


import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import com.shardingjdbc.jpa.tenant.dbhint.config.DynamicDataSource;
import com.shardingjdbc.jpa.tenant.dbhint.entity.User;
import com.shardingjdbc.jpa.tenant.dbhint.entity.query.UserQueryBean;
import com.shardingjdbc.jpa.tenant.dbhint.entity.response.ResponseResult;
import com.shardingjdbc.jpa.tenant.dbhint.service.IUserService;

import java.time.LocalDateTime;

/**
 * @author pdai
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * @param user user param
     * @return user
     */
    @Operation(summary = "Add/Edit User")
    @PostMapping("add")
    public ResponseResult<User> add(User user) {
        DynamicDataSource.setDataSource("shardingDT");
        if (user.getId() == null || !userService.exists(user.getId())) {
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userService.save(user);
        } else {
            user.setUpdateTime(LocalDateTime.now());
            userService.update(user);
        }
        DynamicDataSource.clear();
        return ResponseResult.success(user);
    }


    /**
     * @return user list
     */
    @Operation(summary = "Query User One")
    @GetMapping("edit/{userId}")
    public ResponseResult<User> edit(@PathVariable("userId") Long userId) {
        return ResponseResult.success(userService.find(userId));
    }

    /**
     * @return user list
     */
    @Operation(summary = "Query User Page")
    @GetMapping("list")
    public ResponseResult<Page<User>> list(@RequestParam int pageSize, @RequestParam int pageNumber) {
        return ResponseResult.success(userService.findPage(UserQueryBean.builder().build(), PageRequest.of(pageNumber, pageSize)));
    }
}
