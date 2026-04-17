package com.cmbchina.orgperformance.config;

import com.cmbchina.orgperformance.entity.Institution;
import com.cmbchina.orgperformance.entity.InstitutionLeader;
import com.cmbchina.orgperformance.entity.Skill;
import com.cmbchina.orgperformance.entity.SysRole;
import com.cmbchina.orgperformance.entity.SysUser;
import com.cmbchina.orgperformance.entity.UserSkill;
import com.cmbchina.orgperformance.mapper.InstitutionLeaderMapper;
import com.cmbchina.orgperformance.mapper.InstitutionMapper;
import com.cmbchina.orgperformance.mapper.SkillMapper;
import com.cmbchina.orgperformance.mapper.SysRoleMapper;
import com.cmbchina.orgperformance.mapper.SysUserMapper;
import com.cmbchina.orgperformance.mapper.UserSkillMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class TestDataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private SysRoleMapper roleMapper;
    
    @Autowired
    private InstitutionMapper institutionMapper;
    
    @Autowired
    private InstitutionLeaderMapper leaderMapper;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private UserSkillMapper userSkillMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initRoles();
        initUsers();
        initInstitutions();
        initInstitutionLeaders();
        System.out.println("========== 测试数据初始化完成 ==========");
        printTestAccounts();
    }

    private void initRoles() {
        // 确保角色存在
        List<SysRole> existingRoles = roleMapper.selectAll();
        if (existingRoles.size() < 3) {
            SysRole collectorRole = new SysRole();
            collectorRole.setId(2L);
            collectorRole.setRoleCode("collector");
            collectorRole.setRoleName("收数员");
            collectorRole.setDescription("负责收集绩效数据");
            collectorRole.setCreatedAt(java.time.LocalDateTime.now());
            try { roleMapper.selectById(2L); } catch (Exception e) {
                System.out.println("Creating role: 收数员");
            }

            SysRole leaderRole = new SysRole();
            leaderRole.setId(3L);
            leaderRole.setRoleCode("leader");
            leaderRole.setRoleName("机构负责人");
            leaderRole.setDescription("负责确认本机构绩效数据");
            leaderRole.setCreatedAt(java.time.LocalDateTime.now());
            try { roleMapper.selectById(3L); } catch (Exception e) {
                System.out.println("Creating role: 机构负责人");
            }
        }
    }

    private void initUsers() {
        String encodedPassword = passwordEncoder.encode("admin123");
        
        // 收数员
        createUserIfNotExists(2L, "collector1", encodedPassword, "张三", "C001", "zhangsan@cmbc.com", "13800001001", Arrays.asList(2L));
        createUserIfNotExists(3L, "collector2", encodedPassword, "李四", "C002", "lisi@cmbc.com", "13800001002", Arrays.asList(2L));
        createUserIfNotExists(4L, "collector3", encodedPassword, "王五", "C003", "wangwu@cmbc.com", "13800001003", Arrays.asList(2L));
        
        // 机构负责人
        createUserIfNotExists(5L, "leader1", encodedPassword, "赵六", "L001", "zhaoliu@cmbc.com", "13800001005", Arrays.asList(3L));
        createUserIfNotExists(6L, "leader2", encodedPassword, "钱七", "L002", "qianqi@cmbc.com", "13800001006", Arrays.asList(3L));
        createUserIfNotExists(7L, "leader3", encodedPassword, "孙八", "L003", "sunba@cmbc.com", "13800001007", Arrays.asList(3L));
    }

    private void createUserIfNotExists(Long id, String username, String password, String name,
                                       String empNo, String email, String phone, List<Long> roleIds) {
        try {
            SysUser existing = userMapper.selectById(id);
            if (existing != null) {
                System.out.println("User exists: " + username);
                return;
            }
        } catch (Exception e) {
            // 用户不存在，创建
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setName(name);
        user.setEmpNo(empNo);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());

        try {
            userMapper.insert(user);
            userMapper.insertUserRoles(user.getId(), roleIds);
            System.out.println("Created user: " + username);
            // 自动为新用户安装所有内置技能
            installSkillsForUser(user.getId());
        } catch (Exception e) {
            System.out.println("User already exists or error: " + username);
        }
    }

    /**
     * 为用户安装所有内置技能
     */
    private void installSkillsForUser(Long userId) {
        try {
            List<Skill> builtInSkills = skillMapper.selectBuiltIn();
            for (Skill skill : builtInSkills) {
                UserSkill userSkill = userSkillMapper.selectByUserIdAndSkillId(userId, skill.getId());
                if (userSkill == null) {
                    userSkill = new UserSkill();
                    userSkill.setUserId(userId);
                    userSkill.setSkillId(skill.getId());
                    userSkill.setIsInstalled(1);
                    userSkill.setInstalledAt(java.time.LocalDateTime.now());
                    userSkillMapper.insert(userSkill);
                    System.out.println("  Installed skill: " + skill.getName() + " for user " + userId);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to install skills for user " + userId + ": " + e.getMessage());
        }
    }

    private void initInstitutions() {
        // 创建示例机构
        createInstitutionIfNotExists(1L, 1L, "北京分行", "BJ001", "北方区", "赵六", "L001");
        createInstitutionIfNotExists(2L, 1L, "上海分行", "SH001", "南方区", "钱七", "L002");
        createInstitutionIfNotExists(3L, 1L, "深圳分行", "SZ001", "南方区", "孙八", "L003");
    }

    private void createInstitutionIfNotExists(Long id, Long systemId, String orgName, String orgId,
                                              String groupName, String leaderName, String leaderEmpNo) {
        try {
            List<Institution> existing = institutionMapper.selectBySystemId(systemId);
            for (Institution inst : existing) {
                if (inst.getOrgId().equals(orgId)) {
                    System.out.println("Institution exists: " + orgName);
                    return;
                }
            }
        } catch (Exception e) {
            // continue
        }
        
        Institution inst = new Institution();
        inst.setSystemId(systemId);
        inst.setOrgName(orgName);
        inst.setOrgId(orgId);
        inst.setGroupName(groupName);
        inst.setLeaderName(leaderName);
        inst.setLeaderEmpNo(leaderEmpNo);
        inst.setCreatedAt(java.time.LocalDateTime.now());
        
        try {
            institutionMapper.insert(inst);
            System.out.println("Created institution: " + orgName);
        } catch (Exception e) {
            System.out.println("Institution already exists: " + orgName);
        }
    }

    private void initInstitutionLeaders() {
        // 将机构负责人关联到机构
        createLeaderIfNotExists(1L, 1L, 5L);
        createLeaderIfNotExists(2L, 2L, 6L);
        createLeaderIfNotExists(3L, 3L, 7L);
    }

    private void createLeaderIfNotExists(Long institutionId, Long userId, Long leaderUserId) {
        try {
            InstitutionLeader existing = leaderMapper.selectByInstitutionIdAndUserId(institutionId, leaderUserId);
            if (existing != null) {
                System.out.println("Leader link exists for institution: " + institutionId);
                return;
            }
        } catch (Exception e) {
            // continue
        }
        
        InstitutionLeader leader = new InstitutionLeader();
        leader.setInstitutionId(institutionId);
        leader.setUserId(leaderUserId);
        leader.setConfirmed(false);
        leader.setCreatedAt(java.time.LocalDateTime.now());
        
        try {
            leaderMapper.insert(leader);
            System.out.println("Created leader link: institution=" + institutionId + ", user=" + leaderUserId);
        } catch (Exception e) {
            System.out.println("Leader link already exists or error");
        }
    }

    private void printTestAccounts() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("       测试账号信息 (密码都是 admin123)");
        System.out.println("========================================");
        System.out.println("管理员: admin / admin123");
        System.out.println("收数员: collector1 / collector2 / collector3");
        System.out.println("负责人: leader1 / leader2 / leader3");
        System.out.println("========================================");
        System.out.println("机构负责人关联:");
        System.out.println("  leader1 -> 北京分行 (ID=1)");
        System.out.println("  leader2 -> 上海分行 (ID=2)");
        System.out.println("  leader3 -> 深圳分行 (ID=3)");
        System.out.println("========================================");
    }
}
