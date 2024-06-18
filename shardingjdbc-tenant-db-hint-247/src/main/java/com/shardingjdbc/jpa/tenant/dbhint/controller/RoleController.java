package com.shardingjdbc.jpa.tenant.dbhint.controller;


import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import com.shardingjdbc.jpa.tenant.dbhint.entity.Role;
import com.shardingjdbc.jpa.tenant.dbhint.entity.query.RoleQueryBean;
import com.shardingjdbc.jpa.tenant.dbhint.entity.response.ResponseResult;
import com.shardingjdbc.jpa.tenant.dbhint.service.IRoleService;

import java.time.LocalDateTime;

/**
 * @author pdai
 */
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @Operation(summary = "Add/Edit User")
    @PostMapping("add")
    public ResponseResult<Role> add(Role role) {
        if (role.getId() == null || !roleService.exists(role.getId())) {
            role.setCreateTime(LocalDateTime.now());
            role.setUpdateTime(LocalDateTime.now());
            roleService.save(role);
        } else {
            role.setUpdateTime(LocalDateTime.now());
            roleService.update(role);
        }
        return ResponseResult.success(role);
    }

    /**
     * @return user list
     */
    @Operation(summary = "Query Role List")
    @GetMapping("list")
    public ResponseResult<Page<Role>> list(RoleQueryBean roleQueryBean,
                                           @RequestParam(defaultValue = "1") int pageSize,
                                           @RequestParam(defaultValue = "10") int pageNumber) {
        return ResponseResult.success(roleService.findPage(roleQueryBean, PageRequest.of(pageNumber, pageSize)));
    }
}
