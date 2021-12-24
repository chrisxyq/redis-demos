package com.example.springbootredisdemo;

import com.example.springbootredisdemo.entity.Student;
import com.example.springbootredisdemo.entity.UserEntity;
import com.example.springbootredisdemo.service.StudentService;
import com.example.springbootredisdemo.service.impl.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

@SpringBootTest
@Slf4j
class SpringbootRedisDemoApplicationTests {
    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate       redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private StudentService studentService;
    @Autowired
    private UserService    userService;
    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set("key", "value");
        System.out.println(redisTemplate.opsForValue().get("key"));

    }

    /**
     * stringRedisTemplate使用
     */
    @Test
    public void test() {
        String lockKey = "lockKey";
        final Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "zhuge");
        final Boolean res1 = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "zhuge");
        System.out.println(res);
        System.out.println(res1);
    }

    /**
     * stringRedisTemplate使用
     */
    @Test
    public void test0() {
        // 需要缓存
        // 所有涉及的缓存都需要删除，或者更新
        // 缓存为空的时候，先查,然后缓存redis
    }

    /**
     * 使用json传递对象示例
     *
     * @throws JsonProcessingException
     */
    @Test
    public void test1() throws JsonProcessingException {
        final UserEntity user = new UserEntity("name", "age");
        final String jsonUser = new ObjectMapper().writeValueAsString(user);
        redisTemplate.opsForValue().set("user", jsonUser);
        System.out.println(redisTemplate.opsForValue().get("user"));

    }

    /**
     * 通过Jedis操作redis
     * 是 Redis 的 Java 实现的客户端。支持基本的数据类型如：String、Hash、List、Set、Sorted Set。
     * <p>
     * 特点：使用阻塞的 I/O，方法调用同步，程序流需要等到 socket 处理完 I/O 才能执行，不支持异步操作。Jedis 客户端实例不是线程安全的，
     * 需要通过连接池来使用 Jedis。
     * 通过jedis再次理解事务
     */
    @Test
    public void test2() throws JSONException {
        final Jedis jedis = new Jedis("127.0.0.1", 6379);
        System.out.println(jedis.ping());
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("name", "kuangshen");
        final Transaction multi = jedis.multi();
        final String result = jsonObject.toString();
        try {
            multi.set("user1", result);
            multi.set("user2", result);
            int i = 1 / 0;
            multi.exec();
        } catch (Exception e) {
            multi.discard();
            e.printStackTrace();
        } finally {
            System.out.println(jedis.get("user1"));
        }
    }

    @Test
    public void test3()  {
        Student student1 = this.studentService.queryStudentBySno("001");
        System.out.println("学号" + student1.getSno() + "的学生姓名为：" + student1.getName());

        Student student2 = this.studentService.queryStudentBySno("001");
        System.out.println("学号" + student2.getSno() + "的学生姓名为：" + student2.getName());
    }
    @Test
    public void test5()  {
        this.userService.findUserById(1);
        this.userService.findUserById(1);
    }

    @Test
    public void test4()  {
        Student student1 = this.studentService.queryStudentBySno("001");
        System.out.println("学号" + student1.getSno() + "的学生姓名为：" + student1.getName());

        student1.setName("康康");
        this.studentService.update(student1);

        Student student2 = this.studentService.queryStudentBySno("001");
        System.out.println("学号" + student2.getSno() + "的学生姓名为：" + student2.getName());
    }

}
