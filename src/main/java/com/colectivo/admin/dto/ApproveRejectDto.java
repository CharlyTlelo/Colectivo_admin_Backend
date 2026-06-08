package com.colectivo.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApproveRejectDto {
    private List<String> approvedFields;
    private List<String> rejectedFields;
    private String note;
}
