package com.hdac.contract;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

public abstract class HdacContract
{
	protected SqlSessionFactory sqlSession;

	public void setSqlSessionFactory(SqlSessionFactory SqlSessionFactory)
	{
		sqlSession = SqlSessionFactory;
	}

	protected SqlSession getSqlSession()
	{
		return getSqlSession(true);
	}
	protected SqlSession getSqlSession(boolean autoCommit)
	{
		return sqlSession.openSession(autoCommit);
	}

	public abstract void run();
}