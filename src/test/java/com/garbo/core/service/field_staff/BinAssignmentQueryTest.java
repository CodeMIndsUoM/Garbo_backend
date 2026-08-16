package com.garbo.core.service.field_staff;

import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.CouncilBoundaryRepository;
import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.UserTaskProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class BinAssignmentQueryTest {

    private BinRepository binRepository;
    private BinReportRepository binReportRepository;
    private FieldMentorRepository fieldMentorRepository;
    private CouncilAccessService councilAccessService;
    private CouncilBoundaryRepository councilBoundaryRepository;
    private UserTaskProgressService userTaskProgressService;
    private BinService binService;

    @BeforeEach
    void setUp() {
        binRepository = Mockito.mock(BinRepository.class);
        binReportRepository = Mockito.mock(BinReportRepository.class);
        fieldMentorRepository = Mockito.mock(FieldMentorRepository.class);
        councilAccessService = Mockito.mock(CouncilAccessService.class);
        councilBoundaryRepository = Mockito.mock(CouncilBoundaryRepository.class);
        userTaskProgressService = Mockito.mock(UserTaskProgressService.class);

        binService = new BinService(
                binRepository,
                binReportRepository,
                fieldMentorRepository,
                councilAccessService,
                councilBoundaryRepository,
                userTaskProgressService
        );
    }

    @Test
    void getAssignedBins_returnsBins() {
        Bin bin1 = new Bin();
        bin1.setId(10L);
        bin1.setStatus("empty");

        Bin bin2 = new Bin();
        bin2.setId(11L);
        bin2.setStatus("full");

        when(binRepository.findByAssignedToEmpId(123L)).thenReturn(List.of(bin1, bin2));

        List<Bin> res = binService.getAssignedBins(123L);

        assertEquals(2, res.size());
        assertEquals(10L, res.get(0).getId());
        assertEquals(11L, res.get(1).getId());
    }
}
