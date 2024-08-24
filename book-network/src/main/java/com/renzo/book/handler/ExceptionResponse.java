package com.renzo.book.handler;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionResponse {

    private int bussinessErrorCode;
    private String bussinessDescription;
    private String error;
    private Set<String> validationErrors;
    private Map<String, String> errors;

}
