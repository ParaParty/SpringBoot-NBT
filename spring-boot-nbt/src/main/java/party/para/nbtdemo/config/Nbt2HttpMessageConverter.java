package party.para.nbtdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import party.para.jackson.nbt.NbtMapper;

/**
 * Add NBT support to Spring Boot.
 */
public class Nbt2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {
    /**
     * Construct a new {@link Nbt2HttpMessageConverter} using default configuration
     * provided by {@link Jackson2ObjectMapperBuilder}.
     */
    public Nbt2HttpMessageConverter() {
        this(new NbtMapper());
    }

    /**
     * Construct a new {@link Nbt2HttpMessageConverter} with a custom {@link ObjectMapper}.
     * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
     *
     * @see Jackson2ObjectMapperBuilder#json()
     */
    public Nbt2HttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper, new MediaType("application", "nbt"));
    }

}
