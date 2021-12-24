package com.example.springbootredisdemo.service;

import com.example.springbootredisdemo.entity.Student;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = "student")
public interface StudentService {
	@CachePut(key = "#student.sno")
	Student update(Student student);

	@CacheEvict(key = "#student.sno", allEntries = true)
	void deleteStudentBySno(String sno);
	
	@Cacheable(key = "#sno")
	Student queryStudentBySno(String sno);
}
