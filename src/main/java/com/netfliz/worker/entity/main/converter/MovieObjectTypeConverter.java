package com.netfliz.worker.entity.main.converter;

import com.netfliz.worker.entity.main.enums.MovieObjectType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;

@Converter(autoApply = true)
public class MovieObjectTypeConverter implements AttributeConverter<MovieObjectType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(MovieObjectType movieObjectType) {
        return Objects.isNull(movieObjectType) ? null : movieObjectType.getId();
    }

    @Override
    public MovieObjectType convertToEntityAttribute(Integer integer) {
        return Objects.isNull(integer) ? null : MovieObjectType.fromId(integer);
    }
}
