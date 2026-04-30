package com.garbo.api.mapper;

import com.garbo.api.dto.BinDTO;
import com.garbo.core.entity.Bin;

public class BinMapper {

    public static BinDTO toDTO(Bin bin) {
        BinDTO dto = new BinDTO();
        dto.setId(bin.getId());
       
        dto.setLat(bin.getLat() != null ? bin.getLat() : 0.0);
        dto.setLng(bin.getLng() != null ? bin.getLng() : 0.0);
        dto.setFillLevel(bin.getFillLevel() != null ? bin.getFillLevel() : 0);
        dto.setPriority(bin.getPriority());
        dto.setZone(bin.getZone());
        return dto;
    }

    public static Bin toEntity(BinDTO dto) {
        Bin bin = new Bin();
        bin.setId(null);
        bin.setLatitude(dto.getLat());
        bin.setLongitude(dto.getLng());
        bin.setFillLevel(dto.getFillLevel());
        bin.setPriority(dto.getPriority());
        bin.setZone(dto.getZone());
        return bin;
    }
}


















