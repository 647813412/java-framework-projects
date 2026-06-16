package com.wuyou.youpicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisTemplateTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedisStringOperations(){
        //获取操作对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        //key和value
        String key = "testKey";
        String value = "testValue";

        //1. 测试新增和更新操作
        valueOps.set(key,value);
        String storedValue = valueOps.get(key);
        assertEquals(value,storedValue,"存储的值与预期不一致");

        //2. 测试修改操作
        String updateValue = "updateValue";
        valueOps.set(key,updateValue);
        storedValue = valueOps.get(key);
        assertEquals(updateValue,storedValue,"存储的值与预期不一致");

        //3.测试查询操作
        storedValue = valueOps.get(key);
        assertNotNull(storedValue,"查询值为空");
        assertEquals(updateValue,storedValue,"查询的值与预期不一致");

        //4.测试删除操作
        stringRedisTemplate.delete(key);
        storedValue = valueOps.get(key);
        assertNull(storedValue,"删除后的值不为空");
    }
}
