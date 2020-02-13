package uk.gov.gchq.palisade.service.palisade.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.rule.Rules;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class RulesConverter implements AttributeConverter<Rules<?>, String>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(RulesConverter.class);

    private final ObjectMapper objectMapper;

    public RulesConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(final Rules<?> rules) {
        try {
            return this.objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert rules to json string.", e);
            return null;
        }
    }

    @Override
    public Rules<?> convertToEntityAttribute(final String s) {
        return null;
    }
}
