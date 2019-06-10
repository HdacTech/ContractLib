package com.hdac.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;

/**
 * Common database access object (server, token... etc)
 * 
 * 
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * 
 * @version 0.8
 */
public class CommonDao
{
	private CommonDao()
	{
	}
	public static CommonDao getInstance()
	{
		return LazyHolder.INSTANCE;
	}	  
	private static class LazyHolder
	{
		private static final CommonDao INSTANCE = new CommonDao();  
	}

	public String getServiceInfo(String key, SqlSession sqlSession)
	{
		return sqlSession.selectOne("com.hdac.mapper.CommonMapper.getServiceInfo", key);
	}

	public Map<String, Object> getServerInfo(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.selectOne("com.hdac.mapper.CommonMapper.getServerInfo", paramMap);
	}

	public Map<String, Object> getTokenInfo(SqlSession sqlSession)
	{
		return sqlSession.selectOne("com.hdac.mapper.CommonMapper.getTokenInfo");
	}

	public List<String> getSeed(SqlSession sqlSession)
	{
		return sqlSession.selectList("com.hdac.mapper.CommonMapper.getSeedList");
	}
}