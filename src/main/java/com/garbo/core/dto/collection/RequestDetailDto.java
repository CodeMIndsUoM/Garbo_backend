package com.garbo.core.dto.collection;

import com.garbo.core.entity.CollectionOffer;
import com.garbo.core.entity.CollectionRequest;

import java.util.List;

public record RequestDetailDto(
        RequestSummaryDto request,
        List<OfferDto> offers
) {
    public static RequestDetailDto from(CollectionRequest r, List<CollectionOffer> offers) {
        List<OfferDto> offerDtos = offers.stream().map(OfferDto::from).toList();
        return new RequestDetailDto(RequestSummaryDto.from(r, offers.size()), offerDtos);
    }
}
