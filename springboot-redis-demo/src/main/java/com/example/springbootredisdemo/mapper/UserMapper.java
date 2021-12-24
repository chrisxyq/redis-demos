package com.example.springbootredisdemo.mapper;

import com.example.springbootredisdemo.entity.Student;
import com.example.springbootredisdemo.entity.UserEntity;
import com.example.springbootredisdemo.mapper.MyMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.cache.annotation.CacheConfig;
import tk.mybatis.mapper.common.MySqlMapper;

import javax.annotation.Resource;

@Mapper
@CacheConfig(cacheNames = "UserEntity")
public interface UserMapper extends MyMapper<UserEntity>, MySqlMapper<UserEntity> {

}