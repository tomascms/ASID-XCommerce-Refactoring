package com.xcommerce.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pagination metadata for list responses")
public class PaginationDto {
    @Schema(description = "Current page number (0-indexed)")
    private Integer currentPage;
    
    @Schema(description = "Total number of pages")
    private Integer totalPages;
    
    @Schema(description = "Total number of items")
    private Long totalItems;
    
    @Schema(description = "Number of items per page")
    private Integer pageSize;
    
    @Schema(description = "Whether this is the last page")
    private Boolean isLast;
    
    @Schema(description = "Whether this is the first page")
    private Boolean isFirst;
    
    @Schema(description = "Number of items in current page")
    private Integer numberOfElements;
}

/**
 * PAGINATED RESPONSE DTO - Wrap list responses with pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class PaginatedResponse<T> {
    @Schema(description = "Response success status")
    private Boolean success;
    
    @Schema(description = "Response message")
    private String message;
    
    @Schema(description = "List of items")
    private List<T> data;
    
    @Schema(description = "Pagination metadata")
    private PaginationDto pagination;
    
    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;
}

/**
 * FILTER CRITERIA DTO - Use for search/filter requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter criteria for search operations")
class FilterCriteriaDto {
    @Schema(description = "Field to filter on")
    private String field;
    
    @Schema(description = "Operator: EQ, NE, GT, LT, GTE, LTE, LIKE, IN")
    private String operator;
    
    @Schema(description = "Filter value")
    private Object value;
    
    @Schema(description = "Logical operator: AND, OR")
    private String logicalOperator;
}

/**
 * SORT REQUEST DTO - Use for sorting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sort criteria for responses")
class SortDto {
    @Schema(description = "Field to sort by")
    private String field;
    
    @Schema(description = "Sort direction: ASC or DESC")
    private String direction;
}

/**
 * VALIDATION ERROR DTO - For field-level validation errors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Validation error details")
class ValidationErrorDto {
    @Schema(description = "Field name with error")
    private String field;
    
    @Schema(description = "Rejected value")
    private Object rejectedValue;
    
    @Schema(description = "Error message")
    private String message;
    
    @Schema(description = "Error code")
    private String code;
}

/**
 * ERROR RESPONSE DTO - Enhanced error details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ErrorResponseDto {
    @Schema(description = "Success flag (always false for errors)")
    private Boolean success;
    
    @Schema(description = "HTTP status code")
    private Integer code;
    
    @Schema(description = "Error message")
    private String message;
    
    @Schema(description = "Error type/classification")
    private String errorType;
    
    @Schema(description = "List of validation errors")
    private List<ValidationErrorDto> validationErrors;
    
    @Schema(description = "Error details/stack trace")
    private String details;
    
    @Schema(description = "Request trace ID for debugging")
    private String traceId;
    
    @Schema(description = "Timestamp of error")
    private LocalDateTime timestamp;
}
