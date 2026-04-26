package com.garbo.api.mapper;

import com.garbo.api.dto.BinDTO;
import com.garbo.core.entity.Bin;

public class BinMapper {

    public static BinDTO toDTO(Bin bin) {
        BinDTO dto = new BinDTO();
        dto.id = bin.getId();
        dto.lat = bin.getLat();
        dto.lng = bin.getLng();
        dto.fillLevel = bin.getFillLevel();
        dto.priority = bin.getPriority();
        return dto;
    }

    public static Bin toEntity(BinDTO dto) {
        Bin bin = new Bin(dto.lat, dto.lng, dto.fillLevel, dto.priority);
        
        return bin;
    }
}


















