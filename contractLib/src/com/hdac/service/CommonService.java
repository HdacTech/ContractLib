package com.hdac.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

import com.hdac.comm.HdacUtil;
import com.hdac.dao.CommonDao;
import com.hdacSdk.hdacWallet.HdacWallet;

public class CommonService
{
	private CommonService()
	{
	}
	public static CommonService getInstance()
	{
		return LazyHolder.INSTANCE;
	}	  
	private static class LazyHolder
	{
		private static final CommonService INSTANCE = new CommonService();  
	}

	public Map<String, Object> getTokenInfo(SqlSession sqlSession)
	{
		CommonDao dao = CommonDao.getInstance();
		return dao.getTokenInfo(sqlSession);
	}

	public Map<String, Object> getServerInfo(String serverType, SqlSession sqlSession)
	{
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("serverType", serverType);

		CommonDao dao = CommonDao.getInstance();
		return dao.getServerInfo(paramMap, sqlSession);
	}
	public Map<String, Object> getMainChainInfo(SqlSession sqlSession)
	{
		return getServerInfo("M", sqlSession);
	}
	public Map<String, Object> getSideChainInfo(SqlSession sqlSession)
	{
		return getServerInfo("S", sqlSession);
	}

	public String getServiceInfo(String key, SqlSession sqlSession)
	{
		CommonDao dao = CommonDao.getInstance();
		return dao.getServiceInfo(key, sqlSession);
	}
	public long getMainLoopCount(SqlSession sqlSession)
	{
		return Long.parseLong(getServiceInfo("loop_count_main", sqlSession));
	}
	public long getSideLoopCount(SqlSession sqlSession)
	{
		return Long.parseLong(getServiceInfo("loop_count_side", sqlSession));
	}
	public long getCheckMainBlockHeight(SqlSession sqlSession)
	{
		return Long.parseLong(getServiceInfo("block_count_main", sqlSession));
	}
	public long getCheckSideBlockHeight(SqlSession sqlSession)
	{
		return Long.parseLong(getServiceInfo("block_count_side", sqlSession));
	}
	public String getTransactionFee(SqlSession sqlSession)
	{
		return getServiceInfo("tx_fee", sqlSession);
	}

	public HdacWallet getHdacWallet(SqlSession sqlSession)
	{
		CommonDao dao = CommonDao.getInstance();
		List<String> seedWords = dao.getSeed(sqlSession);
		List<String> seed = HdacUtil.decodeSeed(seedWords, HdacUtil.getKey());
		return HdacUtil.getHdacWallet(seed, null);
	}
}