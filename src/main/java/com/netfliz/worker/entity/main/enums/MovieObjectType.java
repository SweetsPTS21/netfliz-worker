package com.netfliz.worker.entity.main.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MovieObjectType {
    MOVIE(1, "Movie"),
    EPISODE(2, "Episode"),
    SEASON(3, "Season");

    private final Integer id;
    private final String name;

    public static MovieObjectType fromId(Integer id) {
        for (MovieObjectType type : MovieObjectType.values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }
}
