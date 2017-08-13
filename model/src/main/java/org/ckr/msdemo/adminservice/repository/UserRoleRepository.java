package org.ckr.msdemo.adminservice.repository;

import org.ckr.msdemo.adminservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<User, String> {

    @Query(nativeQuery = true,
        value = "select u.* from User u, User_Role ur where ur.ROLE_CODE = ?1 and ur.USER_NAME = u.USER_NAME /* #pageable  */",
        countQuery = "select count(1) from User u, User_Role ur where ur.ROLE_CODE = ?1 and ur.USER_NAME = u.USER_NAME")
    List<User> findUsersByRoleCode(String roleCode, Pageable pageable);

    @Query(nativeQuery = true,
        value = "select u.* from User u, User_Role ur where ur.ROLE_CODE = ?1 and ur.USER_NAME = u.USER_NAME /* #pageable  */",
        countQuery = "select count(1) from User u, User_Role ur where ur.ROLE_CODE = ?1 and ur.USER_NAME = u.USER_NAME")
    Page<User> findUsersByRoleCode2(String roleCode, Pageable pageable);

}