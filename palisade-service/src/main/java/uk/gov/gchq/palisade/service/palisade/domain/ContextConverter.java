package uk.gov.gchq.palisade.service.palisade.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.Context;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.Optional;

@Converter
public class ContextConverter implements AttributeConverter<Context, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextConverter.class);

    private final ObjectMapper objectMapper;

    public ContextConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(Context context) {
        try {
            return this.objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert context to json string.", e);
            return null;
        }
    }

    @Override
    public Context convertToEntityAttribute(String attribute) {
        if (Optional.ofNullable(attribute).isPresent()) {
            try {
                return this.objectMapper.readValue(attribute, Context.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error whilst trying to convert string(JSON) to context.", e);
            }
        }
        return new Context();
    }
}
