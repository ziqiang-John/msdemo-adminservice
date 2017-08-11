package org.ckr.msdemo.adminservice.service;

import org.ckr.msdemo.adminservice.annotation.ReadOnlyTransaction;
import org.ckr.msdemo.adminservice.annotation.ReadWriteTransaction;
import org.ckr.msdemo.adminservice.entity.Role;
import org.ckr.msdemo.adminservice.entity.User;
import org.ckr.msdemo.adminservice.repository.UserRepository;
import org.ckr.msdemo.adminservice.util.StringUtil;
import org.ckr.msdemo.adminservice.valueobject.UserDetailView;
import org.ckr.msdemo.adminservice.valueobject.UserQueryView;
import org.ckr.msdemo.adminservice.valueobject.UserServiceForm;
import org.ckr.msdemo.exception.ApplicationException;
import org.ckr.msdemo.pagination.HibernateRestPaginationService;
import org.ckr.msdemo.pagination.PaginationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.persistence.EntityManagerFactory;

/**
 * User service class.
 * <p><img src="abc.svg" alt="class diagram">
 * <p>abc.
 *
<!--
@startuml abc.svg
Alice -> Bob: Authentication Request
Alice <-- Bob: Authentication Response

Alice -> Bob: Another authentication Request
Alice <-- Bob: another authentication Response


@enduml
' -->
 *
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    HibernateRestPaginationService hibernateRestPaginationService;

    /**
     * Query user detail info by user ID.
     * This method should return the basic user info and the corresponding role info.
     * Please refer {@link UserDetailView} for what kind of user info will be
     * returned.
     * @param userName The user ID.
     * @return detail info of a user.
     * {@link ApplicationException} will be thrown if user is not exist.
     */
    @ReadOnlyTransaction
    public UserDetailView getUser(String userName) {

        UserDetailView result = new UserDetailView();

        User user = this.userRepository.findByUserName(userName);

        if (user == null) {
            ApplicationException exp = new ApplicationException("security.maintain_user.not_existing_user",
                new Object[] {userName});

            exp.addMessage("security.maintain_user.user_name_empty", null);

            throw exp;
        }

        result.setUserName(user.getUserName());
        result.setUserDescription(user.getUserDescription());
        result.setLocked(user.getLocked());

        List<UserServiceForm.RoleServiceForm> roleList = new ArrayList<>();

        for (Role role : user.getRoles()) {

            UserDetailView.RoleDetailView roleView = new UserDetailView.RoleDetailView();

            roleView.setRoleCode(role.getRoleCode());
            roleView.setRoleDescription(role.getRoleDescription());

            roleList.add(roleView);
        }

        result.setRoles(roleList);

        return result;
    }

    @ReadWriteTransaction
    public void createUser(UserServiceForm userForm) {

        LOG.debug("create new user.");

        User user = new User();


        validateUserInfo(userForm);

        if (this.userRepository.findByUserName(userForm.getUserName()) != null) {
            throw new ApplicationException("security.maintain_user.duplicated_user");
        }


        user.setUserName(userForm.getUserName());
        user.setUserDescription(userForm.getUserDescription());
        user.setLocked(Boolean.FALSE);

        user.setPassword(encodePassword(userForm.getPassword()));

        this.userRepository.save(user);

        //this.saveRoles(user, userForm);


    }

    private void validateUserInfo(UserServiceForm user) {
        if (user.getUserName() == null || "".equals(user.getUserName())) {
            throw new ApplicationException("security.maintain_user.user_name_empty");
        }

        if (user.getUserDescription() == null || "".equals(user.getUserDescription())) {
            throw new ApplicationException("security.maintain_user.user_desc_empty");
        }
    }

    private String encodePassword(String pwd) {
        return pwd;
    }

    @ReadOnlyTransaction
    public List<User> queryUsers() {
        PaginationContext.QueryRequest queryRequest = PaginationContext.getQueryRequest();
        int size = queryRequest.getEnd().intValue() - queryRequest.getStart().intValue() + 1;
        int page = queryRequest.getEnd().intValue() / size - 1;
        List<Sort.Order> orders = new ArrayList<>();
        for (PaginationContext.SortCriteria sortCriteria : queryRequest.getSortCriteriaList()) {
            orders.add(new Sort.Order((sortCriteria.isAsc() ? Sort.Direction.ASC : Sort.Direction.DESC), sortCriteria.getFieldName()));
        }
        return userRepository.findAllUsers(new PageRequest(page, size, new Sort(orders)));
    }

    @ReadOnlyTransaction
    public PaginationContext.QueryResponse queryUsers2(String userName, String userDesc) {
        Map<String, Object> params = new HashMap<String, Object>();

        if (!StringUtil.isNull(userName)) {
            params.put("userName", userName);
        }

        if (!StringUtil.isNull(userDesc)) {
            params.put("userDesc", "%" + userDesc + "%");
        }

        String queryStr = "select u.userName, u.userDescription, u.locked from User u where 1=1 "
            + "/*userName| and u.userName = :userName */"
            + "/*userDesc| and u.userDescription like :userDesc */";

        Function<Object[], UserQueryView> mapper = new Function<Object[], UserQueryView>() {

            @Override
            public UserQueryView apply(Object[] row) {

                UserQueryView view = new UserQueryView();

                view.setUserName((String) row[0]);
                view.setUserDescription((String) row[1]);
                view.setLocked(((Boolean) row[2]).toString());

                return view;
            }


        };
        return hibernateRestPaginationService.query(queryStr, params, mapper);

    }


}
