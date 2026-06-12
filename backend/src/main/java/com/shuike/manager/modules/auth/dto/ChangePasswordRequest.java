package com.shuike.manager.modules.auth.dto;
import javax.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ChangePasswordRequest {
    @NotBlank private String oldPassword;
    @NotBlank private String newPassword;
}
