package com.hdac.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

import com.hdac.dao.TokenDao;

public class TokenService
{
	private TokenService()
	{
	}
	public static TokenService getInstance()
	{
		return LazyHolder.INSTANCE;
	}	  
	private static class LazyHolder
	{
		private static final TokenService INSTANCE = new TokenService();
	}

	public int insertTokenInfo(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		TokenDao dao = TokenDao.getInstance();
		long tokenNo = dao.getTokenNo(new HashMap<String, Object>(), sqlSession);
		if (tokenNo > 0)
		{
			paramMap.put("tokenNo", tokenNo);

			return dao.insertTokenInfo(paramMap, sqlSession);
		}
		return -1;
	}
}