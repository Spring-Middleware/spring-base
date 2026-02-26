package com.middleware.mapper;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ViewModelMapperTest {

    private ViewModelMapper<UserRequest, UserViewModel, User> viewModelMapper;

    @Test
    public void testUUIDAndFullname() {

        viewModelMapper = new ViewModelMapper();
        UserRequest userRequest = new UserRequest();
        userRequest.setUuid(UUID.randomUUID());
        userRequest.setName("Name");
        userRequest.setSurname("Surname");
        User user = viewModelMapper.map(userRequest, UserViewModel.class, new User());
        assertEquals(user.getFullname(), "Name Surname");
        assertEquals(user.getUuid(), userRequest.getUuid());
    }



}
