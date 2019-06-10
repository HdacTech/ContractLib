package com.hdac.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import com.hdac.dao.ContractDao;

/**
 * database informations related contract txids
 * 
 * 
 * @see     java.util.HashMap
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * 
 * @version 0.8
 */
public class ContractService
{
	private ContractService()
	{
	}
	public static ContractService getInstance()
	{
		return LazyHolder.INSTANCE;
	}	  
	private static class LazyHolder
	{
		private static final ContractService INSTANCE = new ContractService();
	}

	public long getBlockHeight(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		long height = -1;
		
		ContractDao dao = ContractDao.getInstance();
		
		height = dao.getBlockHeight(paramMap, sqlSession);
		if (height <= 0)
		{
			
		}
		return height;
	}

	public int insertTxList(List<Map<String, Object>> list, long blockHeight, Map<String, Object> chainInfo, SqlSession sqlSession)
	{
		int ret = -1;
		try
		{
			System.out.println(chainInfo);
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("server_type", chainInfo.get("server_type"));
			paramMap.put("list", list);

			for (Map<String, Object> map : list)
				System.out.println(map);

			ContractDao dao = ContractDao.getInstance();
			ret = dao.insertTxList(paramMap, sqlSession);
			if (ret > 0)
			{
				ret = insertBlockHeight(blockHeight, chainInfo, sqlSession);
				if (ret > 0)
					sqlSession.commit();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			sqlSession.rollback();
		}
		return ret;
	}

	public int insertBlockHeight(long blockHeight, Map<String, Object> chainInfo, SqlSession sqlSession)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("server_type", chainInfo.get("server_type"));
		map.put("block_height", blockHeight);

		System.out.println(map);

		ContractDao dao = ContractDao.getInstance();
		return dao.insertBlockHeight(map, sqlSession);		
	}

	public List<Map<String, Object>> getTxList(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		ContractDao dao = ContractDao.getInstance();
		return dao.getTxList(paramMap, sqlSession);
	}

	public int insertHandle(String resultTxid, Map<String, Object> txMap, String type, Map<String, Object> chainInfo, SqlSession sqlSession)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("txid",			resultTxid);
		map.put("block_height",	txMap.get("block_height"));
		map.put("type",			type);
		map.put("origin_txid",	txMap.get("txid"));
		map.put("server_type",	chainInfo.get("server_type"));

		ContractDao dao = ContractDao.getInstance();
		int ret = dao.insertHandle(map, sqlSession);
		if (ret > 0)
		{
			ret = dao.deleteTx(map, sqlSession);
		}
		return ret;
	}

	public int insertRefund(String resultTxid, Map<String, Object> txMap, Map<String, Object> chainInfo, SqlSession sqlSession)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("txid",			resultTxid);
		map.put("block_height",	txMap.get("block_height"));
		map.put("origin_txid",	txMap.get("txid"));
		map.put("server_type",	chainInfo.get("server_type"));

		ContractDao dao = ContractDao.getInstance();
		int ret = dao.insertRefund(map, sqlSession);
		if (ret > 0)
		{
			ret = dao.deleteTx(map, sqlSession);
		}
		return ret;
	}
}