package com.shardingjdbc.jpa.tenant.dbhint.service;

import com.shardingjdbc.jpa.tenant.dbhint.entity.User;
import com.shardingjdbc.jpa.tenant.dbhint.entity.query.UserQueryBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * @author pdai
 */
public interface IUserService extends IBaseService<User, Long> {

    /**
     * find by page.
     *
     * @param userQueryBean query
     * @param pageRequest   pageRequest
     * @return page
     */
    Page<User> findPage(UserQueryBean userQueryBean, PageRequest pageRequest);

}
