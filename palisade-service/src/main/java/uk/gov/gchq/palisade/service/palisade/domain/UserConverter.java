package uk.gov.gchq.palisade.service.palisade.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.User;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.Optional;

@Converter
public class UserConverter implements AttributeConverter<User, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserConverter.class);

    private final ObjectMapper objectMapper;

    public UserConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(User user) {
        try {
            return this.objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert user to json string.", e);
            return null;
        }
    }

    @Override
    public User convertToEntityAttribute(String attribute) {
        if (Optional.ofNullable(attribute).isPresent()) {
            try {
                return this.objectMapper.readValue(attribute, User.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error whilst trying to convert string(JSON) to user.", e);
            }
        }
        return new User();
    }
}
