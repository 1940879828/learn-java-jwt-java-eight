package org.example.jwtjavaeight.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * Web MVC 配置
 * 配置Jackson序列化/反序列化行为
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfiguration.class);

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ========== 反序列化配置 ==========
        // 忽略JSON中存在但Java对象不存在的属性（防止前端多传字段导致错误）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 忽略无法解析的JSON字段
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        // 允许JSON中缺少Java对象中的非必填字段
        mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);

        // 允许空字符串转换为null
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        // ========== 序列化配置 ==========
        // 禁止将日期序列化为时间戳
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // 格式化JSON输出（开发环境可启用，生产环境建议关闭以减少数据量）
        // mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        // 设置日期格式
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 设置时区
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        log.info("[WebMvc] Jackson ObjectMapper配置完成");
        log.debug("[WebMvc] - 忽略未知字段: ENABLED");
        log.debug("[WebMvc] - 日期格式: yyyy-MM-dd HH:mm:ss");
        log.debug("[WebMvc] - 时区: Asia/Shanghai");

        return mapper;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 修改现有的Jackson转换器，而不是替换它
        // 这样不会影响springdoc的OpenAPI文档生成
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                jacksonConverter.setObjectMapper(objectMapper());
                log.info("[WebMvc] 扩展Jackson消息转换器配置");
            }
        }
    }
}
