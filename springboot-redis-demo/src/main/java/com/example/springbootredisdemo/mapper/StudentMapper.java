package com.example.springbootredisdemo.mapper;

import com.example.springbootredisdemo.entity.Student;
import org.apache.ibatis.annotations.*;
import org.springframework.cache.annotation.CacheConfig;
import tk.mybatis.mapper.common.MySqlMapper;

import javax.annotation.Resource;

@Mapper
@CacheConfig(cacheNames = "student")
public interface StudentMapper extends MyMapper<Student>, MySqlMapper<Student> {

	@Update("update student set sname=#{name},ssex=#{sex} where sno=#{sno}")
	int update(Student student);

	@Delete("delete from student where sno=#{sno}")
	void deleteStudentBySno(String sno);

	@Select("select * from student where sno=#{sno}")
	@Results(id = "student", value = { @Result(property = "sno", column = "sno", javaType = String.class),
			@Result(property = "name", column = "sname", javaType = String.class),
			@Result(property = "sex", column = "ssex", javaType = String.class) })
	Student queryStudentBySno(String sno);
}
