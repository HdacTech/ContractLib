package com.hdac.contract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hdac.comm.StringUtil;
import com.hdac.service.CommonService;
import com.hdac.service.RpcService;
import com.hdac.service.TokenService;
import com.hdacSdk.hdacWallet.HdacWallet;

/**
 * Issuing token library
 * 
 * 
 * @see     java.util.HashMap
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.apache.ibatis.session.SqlSession
 * @see     org.json.JSONArray
 * @see     org.json.JSONObject
 * 
 * @version 0.8
 */
public class HdacTokenIssue
{
	private Map<String, Object> mainChain = null;
	private Map<String, Object> sideChain = null;
	private SqlSession sqlSession = null;

	/**
	 * connect database and get chain infomations from database
	 * @param sqlSession (SqlSession) database sql session 
	 * 
	 */			
	public void init(SqlSession sqlSession)
	{
		CommonService service = CommonService.getInstance();
		this.mainChain = service.getMainChainInfo(sqlSession);
		this.sideChain = service.getSideChainInfo(sqlSession);
		this.sqlSession = sqlSession;
	}


	/**
	 * issuing token into side chain
	 * 
	 * @param paramMap (Map(String, Object)) token information before issuing token(address, tokenName, amount, unit etc...)
	 * @param contractAddress (String) contract address
	 * @return    (String) return txid(issued token)
	 */	
	public String issueToken(Map<String, Object> paramMap, String contractAddress)
	{
	    double pointNumber = Double.parseDouble(StringUtil.nvl(paramMap.get("pointNumber"), "0"));

		Map<String, Object> tokenparamMap = new HashMap<String, Object>();
		tokenparamMap.put("address",	contractAddress);
		tokenparamMap.put("tokenName",	paramMap.get("tokenName"));
		tokenparamMap.put("amount",		paramMap.get("tokenCap"));
		tokenparamMap.put("unit",		Math.pow(10, pointNumber * -1));

		System.out.println("tokenParamMap : " + tokenparamMap);
		RpcService service = RpcService.getInstance();
		return service.issueToken(tokenparamMap, this.sideChain); 
	}

	/**
	 * register token info to main chain
	 * 
	 * @param wallet (HdacWallet) hdac wallet with private key
	 * @param paramMap (Map(String, Object)) token informations before register contract
	 * @param contractAddress (String) contract address
	 * @return    (String) return txid(registered contract)
	 */	
	public String registTokenInfo(HdacWallet wallet, Map<String, Object> paramMap, String contractAddress)
	{
		Map<String, Object> contractparamMap = new HashMap<String, Object>();
		contractparamMap.put("addresses", contractAddress);

		RpcService service = RpcService.getInstance();
		List<JSONObject> rpcResult = service.getUtxos(contractparamMap, this.mainChain);

		JSONArray jsonArray = new JSONArray(rpcResult);

		String raw_tx = service.makeRawTransaction(wallet, jsonArray, paramMap);

		// send raw transaction
		return service.sendRawTransaction(raw_tx, this.mainChain);
	}

	/**
	 * register token info to main chain
	 * 
	 * @param paramMap (Map(String, Object)) token informations before insert database
	 * @param sqlSession (SqlSession) sql session
	 * @return    (String) return true if succeeded 
	 */		
	public boolean insertTokenInfo(Map<String, Object> paramMap, SqlSession sqlSession)
	{
		TokenService service = TokenService.getInstance();
		int ret = service.insertTokenInfo(paramMap, sqlSession);
		if (ret > 0)
			return true;
		return false;
	}
	
	/**
	 * register token info to main chain
	 * 
	 * @param paramMap (Map(String, Object)) token informations before insert database
	 * @return    (String) return true if succeeded 
	 */	
	public boolean insertTokenInfo(Map<String, Object> paramMap)
	{
		TokenService service = TokenService.getInstance();
		int ret = service.insertTokenInfo(paramMap, this.sqlSession);
		if (ret > 0)
			return true;
		return false;
	}
}