package com.hdac.dao;

import java.util.Map;
import org.apache.ibatis.session.SqlSession;

import com.hdac.comm.StringUtil;


/**
 * Token database access object (token no, token info)
 * 
 * 
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * 
 * @version 0.8
 */
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