package com.cmbchina.termgoal.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private SysUserVO user;
    private List<String> roles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SysUserVO {
        private Long id;
        private String username;
        private String name;
        private String empNo;
        private String email;
        private Long institutionId;
    }
}
