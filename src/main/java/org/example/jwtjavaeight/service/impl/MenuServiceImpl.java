package org.example.jwtjavaeight.service.impl;

import org.example.jwtjavaeight.domain.entity.SysMenu;
import org.example.jwtjavaeight.mapper.MenuMapper;
import org.example.jwtjavaeight.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public List<SysMenu> findAll() {
        return menuMapper.findAll();
    }

    @Override
    public SysMenu findById(Integer id) {
        return menuMapper.findById(id);
    }

    @Override
    public List<SysMenu> findMenusByRoleId(Integer roleId) {
        return menuMapper.findMenusByRoleId(roleId);
    }

    @Override
    public List<SysMenu> findMenusByUserId(Long userId) {
        return menuMapper.findMenusByUserId(userId);
    }

    @Override
    @Transactional
    public SysMenu create(SysMenu menu) {
        menuMapper.insert(menu);
        return menu;
    }

    @Override
    @Transactional
    public SysMenu update(SysMenu menu) {
        menuMapper.update(menu);
        return menuMapper.findById(menu.getId());
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        menuMapper.deleteById(id);
    }
}
