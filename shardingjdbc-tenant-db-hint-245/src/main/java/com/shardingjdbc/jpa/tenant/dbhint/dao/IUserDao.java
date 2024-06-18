package com.shardingjdbc.jpa.tenant.dbhint.dao;

import com.shardingjdbc.jpa.tenant.dbhint.entity.User;
import org.springframework.stereotype.Repository;

/**
 * @author pdai
 */
@Repository
public interface IUserDao extends IBaseDao<User, Long> {

}
