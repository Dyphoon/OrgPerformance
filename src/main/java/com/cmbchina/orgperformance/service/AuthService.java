package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.dto.LoginRequest;
import com.cmbchina.orgperformance.entity.SysRole;
import com.cmbchina.orgperformance.entity.SysUser;
import com.cmbchina.orgperformance.mapper.SysRoleMapper;
import com.cmbchina.orgperformance.mapper.SysUserMapper;
import com.cmbchina.orgperformance.security.JwtTokenUtil;
import com.cmbchina.orgperformance.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public LoginVO login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SysUser user = userMapper.selectByUsername(request.getUsername());
        String token = jwtTokenUtil.generateToken(user.getUsername());

        List<Long> roleIds = userMapper.selectRoleIdsByUserId(user.getId());
        List<String> roleNames = new ArrayList<>();
        for (Long roleId : roleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role != null) {
                roleNames.add(role.getRoleCode());
            }
        }

        LoginVO.SysUserVO userVO = new LoginVO.SysUserVO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmpNo(),
                user.getEmail(),
                null
        );

        return new LoginVO(token, userVO, roleNames);
    }

    public SysUser getCurrentUser(String username) {
        return userMapper.selectByUsername(username);
    }

    public boolean hasRole(String username, String roleCode) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) return false;

        List<Long> roleIds = userMapper.selectRoleIdsByUserId(user.getId());
        for (Long roleId : roleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role != null && role.getRoleCode().equals(roleCode)) {
                return true;
            }
        }
        return false;
    }
}
