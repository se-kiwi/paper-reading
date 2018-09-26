#Apollo: Scalable and Coordinated Scheduling for Cloud-Scale Computing
##一 简介
###1 大规模调度的要素
- 应对足够大的规模。在一个成千上万的集群上，调度器每秒需要做出成千上万个决策。
- 确保不同用户或组之间资源的共享。
- 确保调度质量（如何充分利用数据的局部性和job的特性等降低延时，提高利用率）。
###2 Apollo的主要技术
- 采用分布式协同调度框架。确保了调度规模以及质量。
- 基于对job完成时间预估的调度。确保了调度质量。
- 利用硬件，将每台服务器上任务调度队列的信息以广播的形式告知Job Manager。
- Apollo采用了一系列延时纠正策略来应对运行过程中的动态变化。
- 引入机会调度策略（ opportunistic scheduling），提高资源利用率。
- 阶段性迁移策略，确保架构无风险迁移。
##二 现实中生产环境下的大规模调度简介
###1 重要概念
- DAG（directed asylic graph）：Apolloc将一个job的执行情况抽象成一个有向无环图。如下图所示，点表示一个任务（task），边表示task之间的数据流。一个job的执行可以划分为若干个stage。
- DOP（degree of parallelism ）：每个stage中并行的task数量。
![DAG](./pics/DAG.png)
###2 容量管理
Apollo采用基于token的容量管理机制。每一个token代表了CPU和内存的固定配额。当为某个job分配内存和CPU时，根据token进行分配。
###3 job调度的基本概念
- 待调度任务列表(ready list)：保存着已准备就绪，等待调度的task。
- 任务优先级(task priority):根据任务的优先级对待调度任务列表中的任务进行调度。
- 容量管理(capacity management)：管理job的容量，并根据基于容量的调度策略决定何时开始调度。
- 任务调度(task scheduling)：决定将task调度至哪一个server。
- 故障恢复(failture recovery)：监视已调度task的状态，如果失败，尝试进行恢复，如果恢复失败，将这个job标记为失败。
- 任务完成(task completion)：当一个task依赖的所有task完成，将这个task放入待调度任务列表。
- 工作完成(job completion)：job中的所有task都完成。
###4 生产工作的特性
在生产环境中，往往要面对成千上万个job，这些job在各个方面都有所不同，比如处理的数据量，处理逻辑等，这对调度框架的伸缩性，高效性，健壮性提出了挑战。
##三 Apollo基本框架
###1 框架总览
![ApplloArchitecture](./pics/ApolloArchitecture.png)
Apollo框架的主要组成部分：
- JM(Job Manager)：每个cluster上有一个，它管理job的生命周期
- RM(Resource Monitor)：收集来自Process Nodes上的负载信息（主要通过），为JM提供整个cluster的状态信息，便于确定job的调度的决策。
- PN(Process Node)：每个sever上有一个本地队列，存储了调度到这个server待执行的task，。PN可以根据这个队列对未来的资源状况进行预测，并将预测结果以wait time matrix的形式发给RM，RM进行整合后连同task的个性化信息一起提供给JM。Wait Time Matrixr如上图所示，描述的是对于特定CPU和内存要求的task在这台server执行前需要的等待的时间。
###2 PN queue和Wait-time Matrix
每一个server上的Process Node管理着一个task队列，储存被分配到这个server上的task。当一个task被调度到某个server上时,JM会发送一个task-creation请求，,同时会发送对内存和CPU的要求,以及预计的运行时间和运行这个task的所需的文件。当server接收到task-creation请求，就会将所需的文件拷贝一份到本地.然后PN监视server的内存以及CPU状态,一旦资源可用，便执行task。因此，task队列并非FIFO，如果前面的task配额不足，后面的task配额足够，便会先调度后面的task。
此外，PN会提供反馈给JM。JM刚开始采用较为保守的估计策略。根据要处理的数据大小以及计算类型估计运行时间。一旦某个task开始运行，鉴于位于同一个stage的task执行的计算类似，运行时间相近，因此PN会监视这个task的运行,响应JM相应的更新请求，将CPU，内存以及I/O吞吐量发送给JM，JM重新估计同一stage的task的运行时间。Waste-time Matrix的情况如上。
###3 基于预估运行时间的调度
####3.1 job运行时间的估计
计算公式：
*E<sub>succ</sub> = I + W + R*
*C = P<sub>succ</sub>E<sub>succ</sub> + K<sub>fail</sub>(1 -  P<sub>succ</sub>)E<sub>succ</sub>*
其中，*E<sub>succ</sub>*表示task不会失败时估计的运行时间，*I*表示读取文件等初始化的时间，*W*表示根据Waste-time Matrix查询目标server的时间，*R*表示task的运行时间,*P<sub>succ</sub>*表示task正常运行的概率，*K<sub>fail</sub>*表示task失败时，penalty常数。
####3.2 Task优先级
job latency不仅与预估的运行时间有关，与task调度顺序也有关。而task得调度顺序取决于task priority。对于DAG中不同的stage,优先级往往不同。根据预估job执行流中的关键路径为每一个stage赋予不同的priority。而对于同一个stage中的task，priority由输入数据得size决定。
####3.3 Stable Matching
对于同一个stage中的task，往往会进行批调度。即将一批task同时调度。常见算法有stable matching和贪心算法。Apollo采用stable matching算法得一个变体，对于每一个task，JM会找到使其预估运行时间最短得server，成为一个proposal，一旦有两个task对同一个server提出proposal，会选择在这个server上运行时间节省最多的那个，另一个会撤销proposal，重新调度，直到所有的task都被调度。
####3.4 校正策略
#####3.4.1 调度发生错误的原因
- 因为分布式的原因，JMs调度决策彼此存在竞争。
- 调度过程中使用的Waste-time Matrix数据可能已过期。因为在实际的生产环境中，server状态可能随时改变。
因此我们需要校正策略帮助我们纠正错误。
#####3.4.2 延时调度的原因
对调度的纠正并非是在调度时就进行操作。因为某些错误的调度并非一定是有害的。如果两个JM将task调度到同一个server，只要server资源充足，便能并行的运行两个task。或者前一个调度至server的task结束的很快，并不影响后面的task。
#####3.4.3 Duplicate Scheduling
对于错误的调度可能存在以下三种情况：
- 新的预估时间远远高于旧的预估时间
- 预估等待时间高于同一个stage的task的平均等待时间
- task已等待时间高于同一个stage的task的平均等待时间
第一种情况表明以前低估了task的运行时间,而对于后两种情况，我们会拷贝一份副本再次调度，如果原来的task开始运行，便会抛弃副本。
#####3.4.5 Randomlization
因为Apollo是一个分布式系统，因此不同的JM可能将task调度到同一个server上，为了避免这种情况，Apollo会在预估结束时间上加上一个小的随机数，减少t不同JM将task调度到同一server的概率。
#####3.4.6 Confidence
因为cluster会保存多个时段的Waste-time Matrix，对于那些过时的Waste-time Matrix，置信度会很低，因此可能会用更高资源请求的waist time进行调度。
#####3.4.7 Straggler
Straggelrs是指那些运行速度远低于其它任务的task，它们会对性能产生严重影响。一旦发现类似的task，且重新运行一份相同task的时间远低于这个task的结束所需时间，就会重新运行一个副本直到task完成或副本进度赶超task进度。如果是因为I/O不正常引起，那么可以更改I/O路径运行副本
####3.5 机会调度(Opportunistic Scheduling)
#####3.5.1 什么是机会调度
Apollo将job分为两种，regular task和opportunistic task。对于regular job而言，它可以在充足的资源环境下运行（达到task的资源要求），而对于opportunistic job而言，它没有确定的资源分配要求，一旦有闲置资源，而待调度任务队列还有opportunistic task未被调度，那么就会将这个task调度到server上，以达到充分利用资源的目的。Opportunistic task的优先级一般比较低，因此不用担心它会抢夺regular task的资源。而且当服务器资源压力较大时，regular task可以抢占opportunistic task甚至终止其运行。
#####3.5.2 为什么需要机会调度
因为在不同时期，服务器负载会有波动。并非在所有时间段内，task都会充分利用分配给它的资源。比如，工作日集群负载要高于周末。此外，job在不同stage对资源的要求也不尽相同。
#####3.5.3 Randomized Allocation Mechanism
Apollo对opportunistic task采用一种随机挑选的方式。为了防止闲置资源被某一个job全部占用，Apollo为每个opportunistic task设定了占用资源的上限。当一个server有闲置资源时，而regular-task queue为空，PN会从opportunistic-task 随机挑选一个task执行。这样可以避免前面的task一直霸占资源，使得所有task都有同样的机会得以运行。
#####3.5.4 Task Upgrade
当server处于负载压力过大的情况下时，低优先级的opportunistic task便有“饿死”(starvation)的风险。为了应对这一问题，Apollo提供了任务升级策略。即在某个时间点将为opportunistic task分配资源使其升级为regular task。一般情况下，调度器会把处于regular task较少，但是opportunistic task等待时间较长的server上的task进行升级。避免影响一些regular task的运行。
