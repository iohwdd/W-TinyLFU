# 背景

对于传统的LRU算法，在面对周期性流量时，容易造成缓存污染。比如数据A在一个周期时间内访问频率很高，此时它位于LRU缓存中的上部（淘汰优先级低），在经过这个周期后，它的访问频率大大降低，此时很容易被其它的缓存顶替下去，最终导致被淘汰。当再次到达数据A访问频率高的周期时，由于已经被淘汰，无法再从缓存中成功获取到。

对于传统的LFU算法，在面对突发性流量时，难以提升流量频率的权重，容易淘汰突发流量数据，造成缓存污染。比如数据B是突发流量，但是这个所谓的突发流量，它的频次在整个缓存中的数据并排不上位，因为旧缓存由于时间的优势，长期以往积累了较多的频次。那么在需要淘汰数据时，突发性的流量将面临着被淘汰的风险。

为弥补这些缺陷，不难想到设计一个整合了LRU+LFU算法设计思想的缓存，达到互补的效果，也就是caffeine底层WTinyLFU算法的设计思想。

# W-TinyLFU

从大方向看，由两部分组成：WindowCache（LRU）与TinyLFU（LFU）

WindoCache作为缓存的入口，新的数据都会先加入这个缓存中

TinyLFU则对WindoCache缓存区中淘汰的数据（记作candidate）进行再缓存，并且内置了两块空间：ProbationCache（观察区）与

ProbationCache（观察区）：对candidate进行直接的交互，若该区未满，candidate直接加入该区即可，反之则会从该区取到频率最低的数据（记作victim）与candidate进行pk，频率较高一方胜利，胜利则留在ProbationCache缓存区中，失败则淘汰数据。当缓存区有一个晋升机制，当访问频率达到一定次数时，即可自动晋升至ProtectedCache区。倘若ProtectedCache区已满，则会将该区频率最低的数据移出作为candidate，然后根据pk淘汰算法觉得是丢弃还是写入ProbationCache。所以，ProbationCache缓存区充当了一个缓冲的作用，以此达到不会急于淘汰数据，高频数据给予更高的保护的效果。

ProtectedCache（保护区）：该缓冲区的数据只能由ProbationCache数据晋升而来。

# 算法整体设计思路图：

![image-20241106185700330](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20241106185700330.png)

# 使用用例

当ProbationCache中数据频率大于两次（可自定义）时，晋升至ProtectedCache
```
public void testPartition() {
        // 各分区容量
        // windowCache 1
        // probationCache 19
        // protectedCache 79
        // 以下测试基于该设定  访问频率>=2 晋升至protected
        WTinyLFU cache = new WTinyLFU(100);
        // 20在window 1~19在probation
        for(int i =1 ;i <= 20; i++){
            cache.put(i+"",i);
        }
        for(int i =1 ;i <= 20; i++){
            System.out.println("cache.isProtected("+ i +")) = " + cache.isProtected(i+""));
            System.out.println("cache.isProbation("+ i +")) = " + cache.isProbation(i+""));
            System.out.println("cache.isWindow("+ i +")) = " + cache.isWindow(i+""));
            System.out.println();
        }
        System.out.println("=================================");
        for(int i =1 ;i <= 20; i++){
            cache.get(i+"");
        }
        // 20在window 1~19在protect
        for(int i =1 ;i <= 20; i++){
            System.out.println("cache.isProtected("+ i +")) = " + cache.isProtected(i+""));
            System.out.println("cache.isProbation("+ i +")) = " + cache.isProbation(i+""));
            System.out.println("cache.isWindow("+ i +")) = " + cache.isWindow(i+""));
            System.out.println();
        }
    }
```
当WindowCache与ProbationCache已满时有新数据进入时的淘汰策略
```
public void testPartition2() {
        WTinyLFU cache = new WTinyLFU(100);
        // 20在window 1~19在protect
        for(int i =1 ;i <= 20; i++){
            cache.put(i+"",i);
        }
        for(int i =1 ;i <= 20; i++){
            cache.get(i+"");
        }
        // 此时probation还有19个位置,下面的数据全在probation
        for(int i = 21; i <= 39 ; i++){
            cache.put(i+"",i);
        }
        // 现在，window，probation都已满。新缓存进来，则会对probation中的victim进行pk来决定是否保留
        cache.put("40",40);
        for(int i =1 ;i <= 40; i++){
            System.out.println("cache.isProtected("+ i +")) = " + cache.isProtected(i+""));
            System.out.println("cache.isProbation("+ i +")) = " + cache.isProbation(i+""));
            System.out.println("cache.isWindow("+ i +")) = " + cache.isWindow(i+""));
            System.out.println();
        }
    }
```
