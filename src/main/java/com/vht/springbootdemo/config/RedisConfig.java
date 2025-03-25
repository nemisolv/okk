package com.vht.springbootdemo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        return new LettuceConnectionFactory(); // Dùng Lettuce thay vì Jedis
//    }




    @Bean
   public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
       RedisTemplate<String, Object> template = new RedisTemplate<>();
       template.setConnectionFactory(factory);


       // Dùng JSON Serializer để lưu object dưới dạng JSON
       template.setKeySerializer(new StringRedisSerializer());
       template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

       // Dùng String serializer cho Hash key
       template.setHashKeySerializer(new StringRedisSerializer());
       template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());


       return template;
   }
}
