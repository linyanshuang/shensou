package com.waimaoshensou.google.search;

import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisPoolFactory {
	private static JedisPool pool;
	private static Log log = LogFactory.getLog(JedisPoolFactory.class);

	private JedisPoolFactory() {

	}

	/**
	 * Jedis Pool for Jedis Resource
	 * 
	 * @return
	 */
	private static JedisPool getJedisPool() {
		if (pool == null) {
			synchronized (JedisPoolFactory.class) {
				if (pool == null) {
					JedisPoolConfig config = new JedisPoolConfig();
					config.setMaxIdle(25);
					config.setMaxTotal(10000);
					config.setMinIdle(5);
					config.setMaxWaitMillis(1000 * 60);
					config.setTestOnBorrow(true);
					config.setNumTestsPerEvictionRun(10);
					config.setMinEvictableIdleTimeMillis(1000 * 60);// 1分钟
					config.setSoftMinEvictableIdleTimeMillis(1000 * 60);// 1分钟
					config.setTimeBetweenEvictionRunsMillis(1000 * 60);// 1分钟
					config.setBlockWhenExhausted(false);
					pool = new JedisPool(config,
							MyProperty.getKey("redis.host"));
				}
			}
		}
		return pool;
	}

	/**
	 * Jedis Pool for Jedis Resource
	 * 
	 * @return
	 */
	private static JedisPool getNewJedisPool() {
		if (pool != null) {
			pool.destroy();
			pool = null;
		}
		synchronized (JedisPoolFactory.class) {
			if (pool == null) {
				JedisPoolConfig config = new JedisPoolConfig();
				config.setMaxIdle(25);
				config.setMaxTotal(250);
				config.setMinIdle(5);
				config.setMaxWaitMillis(1000 * 60);
				config.setTestOnBorrow(true);
				config.setMinEvictableIdleTimeMillis(1000 * 60);// 1分钟
				config.setSoftMinEvictableIdleTimeMillis(1000 * 60);// 1分钟
				config.setTimeBetweenEvictionRunsMillis(1000 * 60);// 1分钟
				config.setBlockWhenExhausted(false);
				pool = new JedisPool(config, MyProperty.getKey("redis.host"));
			}
		}
		return pool;
	}

	public static Jedis getJedis() {
		int timeoutCount = 0;
		while (true) // 如果是网络超时则多试几次
		{
			try {
				Jedis jedis = getJedisPool().getResource();
				return jedis;
			} catch (Exception e) {
				// 底层原因是SocketTimeoutException，不过redis已经捕捉且抛出JedisConnectionException，不继承于前者
				if (e instanceof JedisConnectionException
						|| e instanceof SocketTimeoutException) {
					timeoutCount++;
					log.warn("getJedis timeoutCount=" + timeoutCount);
					if (timeoutCount > 3) {
						break;
					}
				} else {
					log.warn("jedisInfo。NumActive="
							+ getJedisPool().getNumActive() + ", NumIdle="
							+ getJedisPool().getNumIdle() + ", NumWaiters="
							+ getJedisPool().getNumWaiters() + ", isClosed="
							+ getJedisPool().isClosed());
					log.error("getJedis error", e);
					break;
				}
			}
		}
		return null;
	}

	public static Jedis getJedis(boolean newInstance) {
		int timeoutCount = 0;
		while (true) // 如果是网络超时则多试几次
		{
			try {
				Jedis jedis;
				if (newInstance) {
					jedis = getNewJedisPool().getResource();
				} else {
					jedis = getJedisPool().getResource();
				}
				return jedis;
			} catch (Exception e) {
				// 底层原因是SocketTimeoutException，不过redis已经捕捉且抛出JedisConnectionException，不继承于前者
				if (e instanceof JedisConnectionException
						|| e instanceof SocketTimeoutException) {
					timeoutCount++;
					log.warn("getJedis timeoutCount=" + timeoutCount);
					if (timeoutCount > 3) {
						break;
					}
				} else {
					log.warn("jedisInfo。NumActive="
							+ getJedisPool().getNumActive() + ", NumIdle="
							+ getJedisPool().getNumIdle() + ", NumWaiters="
							+ getJedisPool().getNumWaiters() + ", isClosed="
							+ getJedisPool().isClosed());
					log.error("getJedis error", e);
					break;
				}
			}
		}
		return null;
	}

}
