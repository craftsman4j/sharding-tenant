package com.shardingjdbc.jpa.tenant.dbhint.dao;

import org.springframework.stereotype.Repository;
import com.shardingjdbc.jpa.tenant.dbhint.entity.User;

/**
 * @author pdai
 */
@Repository
public interface IUserDao extends IBaseDao<User, Long> {

}
