# 论文阅读报告

## 报告目录

- [Borg]()
- [Kubernetes]()
- [Omega]()
- [Apollo]()
- [Sigma]()

## 对比

| 框架 | 开源/闭源 | 架构 | 多调度器支持 | 优先级抢占策略 | 负载预测
| :---: | :---: | :---: | :---: | :---: | :---:|
| Borg | 闭源 | monolithic | N | 分配时抢占 | Y
| Kubernetes | 开源 | monolithic | N | 分配时抢占 | 
| Omega | 闭源 | shared-state | Y |  | 
| Apollo | 闭源 | shared-state | Y | 分配时抢占 | Y
| Sigma | 闭源 | monolithic | N | 运行时抢占 | Y

## 说明
1. 架构的演进模式为`monolithic`->`two-level`->`shared-state`
    - `monolithic`是最简单的架构，有一个scheduler掌管一切资源，负责所有的调度任务。这样做直观上来看简单统一，但实际上给调度器的设计增加复杂性。`Borg`、`Kubernetes`、`Sigma`都对workload进行分类，最典型的是long-running service jobs和batch analytics jobs，调度器需要针对这些不同的workload分别处理。而且很多时候不同的应用有着不同的调度需求，一套调度系统必须不断增加新的逻辑来支持这些需求。此外，还可能出现[`队头阻塞`](https://en.wikipedia.org/wiki/Head-of-line_blocking)的问题，这也需要谨慎处理。
    - `two-level`将调度划分为两部分：资源分配和任务安置。在这种架构中，存在一个`Resource Manager`负责资源分配，每一个workload可以拥有特定的Scheduler，这些workload级别的scheduler会向`Resource Manager`申请资源，再由Scheduler在申请到的资源中安置具体的任务。典型的如`Mesos`采用这种架构，它虽然能解决支持多调度器的问题，但会导致`优先级抢占`变得十分困难，因为每个workload级别的scheduler无法得知全局的资源利用情况。
    - `shared-state`为解决上述问题，将全局资源的使用情况独立出来，并在每一个scheduler中储存一个副本，并且每一个scheduler都可以利用这些资源利用情况做出最优的选择。`Omega`和`Apollo`都采用这种设计，这样做可以达到很高的并发性，却需要很多额外的工作保证一致性，以至于在很多情况下state副本中的信息不是最新的，会导致conflict以影响性能。

2. `Sigma`在很大程度上借鉴了`Borg`的做法，比如中央式架构、服务混部利用策略等等。`Sigma`又融合了`Kubernetes`的许多思想，采用容器技术，而不是像`Borg`那样直接跑在物理机上。`Sigma`还有自己的一套容器系统：`Pouch`，并兼容`Kubernetes`API。

3. 相较于`brog` 与 `Apollo`, `sigma` 更加关注于资源池的扩缩容和调度系统的弹性，而不是追求资源利用率的最大化。这和阿里双十一的业务需求的特点也相契合。

4. 这五种框架，只有`Kubernetes`开源，除了`Omega`的其他几个框架都得到了大规模商用：`Borg`(Google)，`Apollo`(Microsoft)，`Sigma`(阿里巴巴)。`Omega`由Google开发，但其特性不足以像`Borg`那样支持数以千万的机器集群，目前只能作为`Borg`的附属子系统工作。

5. 从`Borg`来看，集中化也并不是性能的瓶颈，`Kubernetes`在`Borg`的基础上做了许多优化，比如引入了包含多任务的工作流，便于追踪与分配资源；更灵活的IP策略，而不是为每一台机器分配一个IP；更简单的部署方法，只需要书写简单的`yaml`配置文件即可完成部署。


------------------------
## 参考文献
- [1] [The evolution of cluster scheduler architectures](http://www.firmament.io/blog/scheduler-architectures.html)
- [2] [Mesos, Omega, Borg: A Survey](https://www.umbrant.com/2015/05/27/mesos-omega-borg-a-survey/)