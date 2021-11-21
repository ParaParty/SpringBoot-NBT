package party.para.nbtdemo.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring Boot NBT support auto configuration.
 */
@Configuration
@ConditionalOnClass({WebMvcConfigurer.class, HttpMessageConverter.class})
public class BaseMvcAutoConfiguration implements WebMvcConfigurer, InitializingBean {
    @SuppressWarnings("RedundantThrows")
    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new Nbt2HttpMessageConverter());
    }
}