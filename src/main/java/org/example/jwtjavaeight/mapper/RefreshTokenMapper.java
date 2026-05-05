package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.entity.SysRefreshToken;

@Mapper
public interface RefreshTokenMapper {

  int insert(SysRefreshToken refreshToken);

  SysRefreshToken findByTokenHash(@Param("tokenHash") String tokenHash);

  int deleteByUserId(@Param("userId") Long userId);

  int deleteByJtiId(@Param("jtiId") String jtiId);
}
