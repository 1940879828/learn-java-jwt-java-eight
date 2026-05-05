package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.jwtjavaeight.domain.entity.SysLoginLog;

@Mapper
public interface LoginLogMapper {

  int insert(SysLoginLog log);
}
