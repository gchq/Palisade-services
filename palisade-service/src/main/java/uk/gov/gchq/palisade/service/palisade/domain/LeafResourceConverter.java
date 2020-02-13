package uk.gov.gchq.palisade.service.palisade.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.Optional;

@Converter
public class LeafResourceConverter implements AttributeConverter<LeafResource, String>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeafResourceConverter.class);

    private final ObjectMapper objectMapper;

    public LeafResourceConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(LeafResource leafResource) {
        try {
            return this.objectMapper.writeValueAsString(leafResource);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert leafresource to json string.", e);
            return null;
        }
    }

    @Override
    public LeafResource convertToEntityAttribute(String attribute) {
        if (Optional.ofNullable(attribute).isPresent()) {
            try {
                return this.objectMapper.readValue(attribute, LeafResource.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error whilst trying to convert string(JSON) to leafresource.", e);
            }
        }
        return (LeafResource) new DirectoryResource().id("//");
    }
}
