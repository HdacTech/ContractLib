package com.hdac.dao;

import java.util.Map;
import org.apache.ibatis.session.SqlSession;

import com.hdac.comm.StringUtil;

public class TokenDao
{
	private TokenDao()
	{
	}
	public static TokenDao getInstance()
	{
		return LazyHolder.INSTANCE;
	}	  
	private static class LazyHolder
	{
		private static final TokenDao INSTANCE = new TokenDao();  
	}

	public long getTokenNo(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		sqlSession.insert("com.hdac.mapper.TokenMapper.insertTokenNo", paramMap);
		return Long.parseLong(StringUtil.nvl(paramMap.get("seq_val"), "0"));
	}

	public int insertTokenInfo(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.insert("com.hdac.mapper.TokenMapper.insertTokenInfo", paramMap);
	}
}