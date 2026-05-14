package org.example.jwtjavaeight.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.jwtjavaeight.domain.entity.SysRefreshToken;

@Mapper
public interface RefreshTokenMapper {

  void insert(SysRefreshToken refreshToken);

  SysRefreshToken findByTokenHash(@Param("tokenHash") String tokenHash);

  void deleteByUserId(@Param("userId") Long userId);

  void deleteByJtiId(@Param("jtiId") String jtiId);
}
