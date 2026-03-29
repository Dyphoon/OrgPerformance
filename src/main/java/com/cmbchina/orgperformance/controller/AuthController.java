package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.dto.LoginRequest;
import com.cmbchina.orgperformance.security.CustomUserDetailsService;
import com.cmbchina.orgperformance.service.AuthService;
import com.cmbchina.orgperformance.vo.ApiResponse;
import com.cmbchina.orgperformance.vo.LoginVO;
import com.cmbchina.orgperformance.mapper.InstitutionLeaderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private InstitutionLeaderMapper institutionLeaderMapper;

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody LoginRequest request) {
        LoginVO loginVO = authService.login(request);
        return ApiResponse.success(loginVO);
    }

    @GetMapping("/current-user")
    public ApiResponse<LoginVO.SysUserVO> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ApiResponse.error(401, "Not authenticated");
        }
        String username = authentication.getName();
        com.cmbchina.orgperformance.entity.SysUser user = authService.getCurrentUser(username);
        if (user == null) {
            return ApiResponse.error(404, "User not found");
        }
        LoginVO.SysUserVO userVO = new LoginVO.SysUserVO(
                user.getId(), user.getUsername(), user.getName(), user.getEmpNo(), user.getEmail(),
                institutionLeaderMapper.selectByUserId(user.getId()).stream().findFirst().map(il -> il.getInstitutionId()).orElse(null)
        );
        return ApiResponse.success(userVO);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.success("Logged out successfully");
    }
}
