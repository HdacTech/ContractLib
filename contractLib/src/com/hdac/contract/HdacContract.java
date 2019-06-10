package com.hdac.contract;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * abstract HdacContract class
 * (Database SqlSesseion control) 
 * 
 * @version 0.8
 * @see     org.apache.ibatis.session.SqlSession
 * @see     org.apache.ibatis.session.SqlSessionFactory
 *
 */
public abstract class HdacContract
{
	protected SqlSessionFactory sqlSession;

	/**
	 * set SqlSessionFactory
	 * 
	 * @param SqlSessionFactory	SqlSessionFactory
	 */	
	public void setSqlSessionFactory(SqlSessionFactory SqlSessionFactory)
	{
		sqlSession = SqlSessionFactory;
	}

	/**
	 * get SqlSession
	 * 
	 * @return    (SqlSession) current SqlSession
	 */	
	protected SqlSession getSqlSession()
	{
		return getSqlSession(true);
	}
	
	/**
	 * get SqlSession
	 * 
	 * @param autoCommit (boolean)open sesseion
	 * @return    (SqlSession) current SqlSession
	 */
	protected SqlSession getSqlSession(boolean autoCommit)
	{
		return sqlSession.openSession(autoCommit);
	}

	public abstract void run();
}