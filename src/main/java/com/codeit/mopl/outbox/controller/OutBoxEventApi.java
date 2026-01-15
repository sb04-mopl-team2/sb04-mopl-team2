package com.codeit.mopl.outbox.controller;

import com.codeit.mopl.outbox.dto.CursorResponseOutBoxEventDto;
import com.codeit.mopl.outbox.dto.OutBoxEventDto;
import com.codeit.mopl.outbox.dto.OutBoxSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "OutBox", description = "OutBox API")
public interface OutBoxEventApi {

    @Operation(summary = "OutBox 목록 조회")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200", description = "OutBox 목록 조회 성공",
                content = @Content(schema = @Schema(implementation = CursorResponseOutBoxEventDto.class))
        )
    })
    ResponseEntity<CursorResponseOutBoxEventDto> getOutBoxEvents(
            @Parameter(description = "페이징 정보") OutBoxSearchRequest request
    );

    @Operation(summary = "OutBox 재시도")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "OutBox 재시도 성공",
                    content = @Content(schema = @Schema(implementation = OutBoxEventDto.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "OutBox를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "OutBox 이벤트를 찾을 수 없습니다."))
            )
    })
    ResponseEntity<OutBoxEventDto> retryOutBoxEvent(
            @Parameter(description = "재시도 할 OutBox ID") UUID outboxEventId
    );
    
    @Operation(summary = "OutBox 일괄 재시도")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "OutBox 일괄 재시도 성공",
                    content = @Content(schema = @Schema(implementation = OutBoxEventDto.class))
            )
    })
    ResponseEntity<List<OutBoxEventDto>> retryAllDeadOutBoxEvents(
            @Parameter(description = "일괄 재시도 limit") int limit
    );

}
