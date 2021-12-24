package com.example.springbootredisdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity implements Serializable {
    private Integer id;
    private String name;
    private String age;

    public UserEntity(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public UserEntity(String name, String age) {
        this.name = name;
        this.age = age;
    }
}
