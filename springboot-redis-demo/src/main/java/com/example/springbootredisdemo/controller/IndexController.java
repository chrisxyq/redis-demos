package com.example.springbootredisdemo.controller;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class IndexController {
    @Autowired
    private Redisson            redisson;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 单机锁会出现超卖现象
     *
     * @return
     */
    @RequestMapping("/deductStock")
    public String deductStock() {
        synchronized (this) {
            final int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
            return "end";
        }
    }

    /**
     * 考虑：单机锁超卖
     * 解决：使用redis分布式锁
     * setnx key value:当key已经存在。redis不做任何操作
     * redis为单线程的，抢到redis锁就执行业务代码
     * 抢不到redis锁就直接拒绝
     *
     * @return
     */
    @RequestMapping("/deductStock1")
    public String deductStock1() {
        String lockKey = "product001";
        //jedis.setnx(k,v);
        final Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "zhuge");
        if (!res) {
            return "errorCode";
        }
        final int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
        if (stock > 0) {
            int realStock = stock - 1;
            stringRedisTemplate.opsForValue().set("stock", realStock + "");
            System.out.println("扣减成功，剩余库存：" + realStock);
        } else {
            System.out.println("扣减失败，库存不足");
        }
        stringRedisTemplate.delete(lockKey);
        return "end";
    }

    /**
     * 考虑：若拿到锁的线程在执行业务代码时，抛异常。则redis锁无法释放。
     * 导致后续线程在执行setIfAbsent时，res永远返回false
     * 则一直无法执行业务代码
     * 解决：try finally
     *
     * @return
     */
    @RequestMapping("/deductStock2")
    public String deductStock2() {
        String lockKey = "product001";
        try {
            //jedis.setnx(k,v);
            final Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "zhuge");
            if (!res) {
                return "errorCode";
            }
            final int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
        return "end";
    }

    /**
     * 考虑：机器宕机，依旧无法删除锁
     * 解决：为锁加过期时间
     *
     * @return
     */
    @RequestMapping("/deductStock3")
    public String deductStock3() {
        String lockKey = "product001";
        try {
            //jedis.setnx(k,v);
            final Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "zhuge");
            stringRedisTemplate.expire(lockKey, 10, TimeUnit.SECONDS);
            if (!res) {
                return "errorCode";
            }
            final int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
        return "end";
    }

    /**
     * 考虑：获取锁和为锁加过期时间的操作不是原子操作
     * 解决：改为原子操作
     * io.lettuce.core.RedisCommandExecutionException: ERR wrong number of arguments for 'set' command
     * 仅支持redis2.6.12之后的版本
     *
     * @return
     */
    @RequestMapping("/deductStock4")
    public String deductStock4() {
        String lockKey = "product001";
        try {
            //jedis.setnx(k,v);
            final Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "zhuge", 10, TimeUnit.SECONDS);
            if (!res) {
                return "errorCode";
            }
            final int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
        return "end";
    }

    /**
     * 考虑：过期时间设置不合理
     * 过期时间设置过长：可能删除别人的锁：线程A执行完，finally删除了自己的锁，过了一会timeout开始删除锁（这时下一个线程可能还没执行完）
     * <p>
     * 解决方案1：
     * 1.设置锁的value为uuid或者requestid
     * 2.守护线程定时任务检查锁是否存在，如果存在，则延期
     * 或者使用redisson
     * <p>
     * ps：为了解决大量锁失败的问题，引入自旋锁
     * 在规定的时间，比如500毫秒内，自旋不断尝试加锁（说白了，就是在死循环中，不断尝试加锁），
     * 如果成功则直接返回。如果失败，则休眠50毫秒，再发起新一轮的尝试。如果到了超时时间，还未加锁成功，则直接返回失败。
     *
     * @return
     */
    @RequestMapping("/deductStock5")
    public String deductStock5() {
        String lockKey = "product001";
        final String clientId = UUID.randomUUID().toString();
        try {
            Long start = System.currentTimeMillis();
            //jedis.setnx(k,v);
            final Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "zhuge", 10, TimeUnit.SECONDS);
            if (!res) {
                long time = System.currentTimeMillis() - start;
                if (time >= 500) {
                    return "errorCode";
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            final int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientId.equals(stringRedisTemplate.opsForValue().get(lockKey))) {
                //确定是本线程的锁
                stringRedisTemplate.delete(lockKey);
            }
        }
        return "end";
    }

    /**
     * 考虑：过期时间设置不合理
     * 过期时间设置过长：可能删除别人的锁：线程A执行完，finally删除了自己的锁，过了一会timeout开始删除锁（这时下一个线程可能还没执行完）
     * <p>
     * 解决方案2：
     * 或者使用redisson
     * 获取不到redisson锁的线程自旋
     * 获取到redisson锁的线程，由守护线程自动续命
     * <p>
     * 备注：redisson不仅为了解决大量锁失败问题，引入自旋锁
     * 而且为了解决redis分布式锁不可重入的问题，使用可重入锁解决递归场景下获取锁失败的问题
     * 因为还有这样的场景：
     * 假设在某个请求中，需要获取一颗满足条件的菜单树或者分类树。我们以菜单为例，这就需要在接口中从根节点开始，递归遍历出所有满足条件的子节点，
     * 然后组装成一颗菜单树。
     * 需要注意的是菜单不是一成不变的，在后台系统中运营同学可以动态添加、修改和删除菜单。为了保证在并发的情况下，每次都可能获取最新的数据，
     * 这里可以加redis分布式锁。
     * 加redis分布式锁的思路是对的。但接下来问题来了，在递归方法中递归遍历多次，每次都是加的同一把锁。递归第一层当然是可以加锁成功的，
     * 但递归第二层、第三层...第N层，不就会加锁失败了？
     *
     * @return
     */
    @RequestMapping("/deductStock6")
    public String deductStock6() {
        String lockKey = "product001";
        final RLock redissonLock = redisson.getLock(lockKey);
        try {
            redissonLock.lock();
            final int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            redissonLock.unlock();
        }
        return "end";
    }

    /**
     * 在绝大多数实际业务场景中，一般是读数据的频率远远大于写数据。
     * 而线程间的并发读操作是并不涉及并发安全问题，我们没有必要给读操作加互斥锁，
     * 只要保证读写、写写并发操作上锁是互斥的就行，这样可以提升系统的性能。
     *
     * 读与读是共享的，不互斥
     * 读与写互斥
     * 写与写互斥
     *
     * @return
     */
    @RequestMapping("/deductStock7")
    public String deductStock7() {
        String lockKey = "product001";
        //读锁
        RReadWriteLock readWriteLock = redisson.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();
        try {
            readLock.lock();
            //业务操作
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readLock.unlock();
        }
        //写锁
        RLock writeLock = readWriteLock.writeLock();
        try {
            writeLock.lock();
            //业务操作
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
        //自动续期任务
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            /**
             * The action to be performed by this timer task.
             */
            @Override
            public void run() {

            }
        }, 10000);
        return "end";
    }
    /**
     * 考虑：redis集群背景下，redis主节点还没把key同步给从节点，主节点就挂了
     * 由于redis的选举机制，从节点变成主节点，此时redis集群没有这个key，因此其他线程依旧可以获取锁
     * 会出现两个线程同时执行业务代码的情况
     * <p>
     * 知识：CAP
     * redis：单机上万QPS
     * zookeeper:(半数节点同步成功，zookeeper才会获取锁成功)为了一致性，牺牲可用性
     * <p>
     * 解决：redisson框架为了解决这个问题，提供了一个专门的类：RedissonRedLock，使用了Redlock算法：
     * 超过半数redis节点加锁成功，才算成功（其实少用，不推荐）
     * 此外：如何提升分布式锁的性能
     * @return
     */
}
