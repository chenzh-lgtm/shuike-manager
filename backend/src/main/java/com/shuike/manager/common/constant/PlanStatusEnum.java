package com.shuike.manager.common.constant;

public enum PlanStatusEnum {
    DRAFT("草稿"),
    SUBMITTED("待学院审核"),
    COLLEGE_PASSED("待教务处审核"),
    APPROVED("已通过"),
    REJECTED("待修改");

    private final String label;
    PlanStatusEnum(String label) { this.label = label; }
    public String getLabel() { return label; }
}
