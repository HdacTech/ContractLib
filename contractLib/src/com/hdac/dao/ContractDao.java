package com.hdac.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;

/**
 * Contract database access object (blockheight, handling txid...)
 * 
 * 
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * 
 * @version 0.8
 */
public class ContractDao
{
	private ContractDao()
	{
	}
	public static ContractDao getInstance()
	{
		return LazyHolder.INSTANCE;
	}	  
	private static class LazyHolder
	{
		private static final ContractDao INSTANCE = new ContractDao();  
	}

	public long getBlockHeight(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.selectOne("com.hdac.mapper.ContractMapper.getBlockHeight", paramMap);
	}

	public int insertTxList(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.insert("com.hdac.mapper.ContractMapper.insertTxList", paramMap);
	}

	public int insertBlockHeight(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.insert("com.hdac.mapper.ContractMapper.insertBlockHeight", paramMap);
	}

	public List<Map<String, Object>> getTxList(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.selectList("com.hdac.mapper.ContractMapper.getTxList", paramMap);
	}

	public int insertHandle(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.insert("com.hdac.mapper.ContractMapper.insertHandle", paramMap);
	}

	public int deleteTx(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.delete("com.hdac.mapper.ContractMapper.deleteTx", paramMap);
	}

	public int insertRefund(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		return sqlSession.insert("com.hdac.mapper.ContractMapper.insertRefund", paramMap);
	}
}