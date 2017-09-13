package com.zg.ms.thread;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * Created by lenovo on 2017/9/12.
 * 声明一个多线程
 */
public class MyRunnable implements Runnable {

    //watchkeys为redis的命令
    String watchkeys = "watchkeys";// 监视keys
    Jedis jedis = new Jedis("192.168.36.129", 6379);
    //属性+有参无参构造方法
    String userinfo;
    public MyRunnable() {
    }
    public MyRunnable(String uinfo) {
        this.userinfo=uinfo;
    }

    //重写线程的run方法
    @Override
    public void run() {

        try {
            jedis.watch(watchkeys);// watchkeys监控key

            String val = jedis.get(watchkeys);//得到key的值
            int valint = Integer.valueOf(val);

            //判断获取redis中的key的value值是否在100以内
            if (valint <= 100 && valint>=1) {

                Transaction tx = jedis.multi();// 开启事务
                // tx.incr("watchkeys");
                /**
                 * Redis Incrby 命令将 key 中储存的数字加上指定的增量值。
                 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCRBY 命令。
                 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
                 本操作的值限制在 64 位(bit)有符号数字表示之内。
                 */
                tx.incrBy("watchkeys", -1);

                List<Object> list = tx.exec();// 提交事务，如果此时watchkeys被改动了，则返回null

                if (list == null ||list.size()==0) {

                    String failuserifo = "fail"+userinfo;
                    String failinfo="用户：" + failuserifo + "商品争抢失败，抢购失败";
                    System.out.println(failinfo);

                    /* 抢购失败业务逻辑 jedis.set(watchkeys, "100");//设置起始的抢购数*/
                    /**
                     * Redis有一系列的命令，特点是以NX结尾，NX是Not eXists的缩写，如SETNX命令就应该理解为：SET if Not eXists。这系列的命令非常有用，这里讲使用SETNX来实现分布式锁。

                     　　直接上重点：

                     　　SET NX 命令是快速失败锁，就是当第一次设置key和value时返回1，当第二次设置相同的key时，返回0，此时对原值不做任何更改。
                     */

                  jedis.setnx(failuserifo, failinfo);
                    //jedis.set(failuserifo, failinfo);
                    System.out.println(failuserifo+"=========失败:"+jedis.get(failuserifo));
                } else {
                    for(Object succ : list){
                        String succuserifo ="succ"+succ.toString() +userinfo ;
                        String succinfo="用户：" + succuserifo + "抢购成功，当前抢购成功人数:"
                                + (1-(valint-100));
                        System.out.println(succinfo);

                         /* 抢购成功业务逻辑 */
                        jedis.setnx(succuserifo, succinfo);
                        System.out.println(succuserifo+"=========成功:"+jedis.get(succuserifo));
                    }

                }

            } else {
                String failuserifo ="kcfail" +  userinfo;
                String failinfo1="用户：" + failuserifo + "商品被抢购完毕，抢购失败";
                System.out.println(failinfo1);
                jedis.setnx(failuserifo, failinfo1);
                // Thread.sleep(500);  //注意failuserifo本身就是一个字符串,不用加双引号
                System.out.println(failuserifo+"============="+jedis.get(failuserifo));
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }


    }
}
