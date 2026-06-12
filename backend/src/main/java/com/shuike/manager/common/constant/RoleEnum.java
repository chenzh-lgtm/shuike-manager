package com.shuike.manager.common.constant;

public enum RoleEnum {
    TEACHER("TEACHER", "教师"),
    COLLEGE_REVIEWER("COLLEGE_REVIEWER", "二级学院负责人审核员"),
    OFFICE("OFFICE", "教务处"),
    DEAN("DEAN", "院长/专业负责人");

    private final String code;
    private final String label;

    RoleEnum(String code, String label) { this.code = code; this.label = label; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
}
