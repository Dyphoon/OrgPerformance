package com.cmbchina.orgperformance.security;

import com.cmbchina.orgperformance.entity.SysUser;
import com.cmbchina.orgperformance.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private SysUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        List<Long> roleIds = userMapper.selectRoleIdsByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Long roleId : roleIds) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleId));
        }

        return new User(user.getUsername(), user.getPassword(), authorities);
    }

    public SysUser getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
}
