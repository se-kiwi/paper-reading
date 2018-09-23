# BORG, A Large-Scale Cluster Management System

>Google's Borg system is a cluster manager that runs hundreds of thousands of jobs, from many thousands of different applications, across a number of clusters each with up to tens of thousands of machines.

## Feature Introduction

### It achieves high utilization by

+ combining admission control
+ efficient task-packing
+ over-commitment
+ machine sharing with process-level performance

### It achieves high-availability by runtime features
+ minimize fault-recovery time
+ scheduling policies that reduce the probability of correlated failures. 

### Main benefit
+ hide the details of resource management and failure handling so its user can focus on application development instead
+ operates with very high reliability and availability, and supports applications that do the same
+ lets us run workloads across tens of thousands of machines effectively

![Figure 1:](./borg_assest/figure1.png)


## User perspective

### The workload

It divide into two classes by their running time and latency requirement

----
>The first is long-running services that should “never” go
down, and handle short-lived latency-sensitive requests (a
few µs to a few hundred ms)

For example, Gmail, WebService and a order distribution system.

We classify this kind of jobs as "production" (`prod`)

>In a representative cell, prod jobs
are allocated about 70% of the total CPU resources and represent about 60% of the total CPU usage; they are allocated about 55% of the total memory and represent about 85% of the total memory usage. 

----
>The second is batch jobs that take from a few seconds to a few days to complete; these are much less sensitive
to short-term performance fluctuations

For example, some scientific calculation, AI training and web spider.

We classify this kind of jobs as "non-production"(`non-prod`)

### Clusters and cells

A cluster is in a datacenter building.

Many building make up a site.

A cluster usually hosts one large cell and may have a few smaller-scale test or special-purpose cells.

>Our median cell size is about 10 k machines after excluding test cells; some are much larger. The machines in a cell are heterogeneous in many dimensions: sizes (CPU, RAM, disk, network), processor type, performance, and capabilities such as an external IP address or flash storage. Borg isolates users from most of these differences by determining where in a cell to run tasks, allocating their resources, installing their programs and other dependencies, monitoring their health, and restarting them if they fail.

### Jobs and tasks

Jobs properties include name, owner, the number of tasks. 

Jobs can have constraints to force its tasks to run on some machine that satisfy the needs. 

A job can be deferred until a prior one finished, and it runs in just one cell.

Each task maps to a set of Linux process running in a container to avoid cost of virtualization.

A task has properties, such as resource requirement and its info. 

![Figure 2](./borg_assest/figure2.png )

A user can operate jobs by issuing remote procedure call to Borg, most through a command-line. And user can change the properties of some of all of the tasks in a running jobs by pushing new configuration, and then Borg will update to new specification. This action is lightweight, non-atomic so can be easily be undone until it is closed. 

A task may restart because the requirement change. It will be notified via a Unix `SIGTERM` before they are preempted by `SIGKILL`.

### Allocs

>A Borg *alloc* (short for allocation) is a reserved set of resources on a machine in which one or more tasks can be run; the resources remain assigned whether or not they are used.

It can be used to retain resource and gather tasks.

The resources of an alloc are treated as resources of a machine.

### Priority, quota and admission control

Each job has a *priority*. It expresses relative importance for jobs that are running or waiting ro run in a cell. A high-priority task can obtain resource at the expense of a lower-priority one, even it will preempting the latter. 

Borg defines non-overlapping *priority* bands for different users, including: `monitoring`, `production`, `batch`, `best effort`(as known as `testing` or `free`). `prod` is `monitoring` and `production`.

To eliminate *preemption cascades*, Borg disallow `production` priority band to preempt one another.

----

Quota is used to decide which jobs to *admit* for scheduling. 

> Quota is expressed as
a vector of resource quantities (CPU, RAM, disk, etc.) at a
given priority, for a period of time (typically months).

Higher-priority quota costs more than quota a lower-priority.

Because people alway overbuy quota to avoid future shortage, Borg take over-selling at lower-priority levels. So that a low-priority jobs may be admitted but remain pending.

Quota allocation is handled outside of Borg, it is related to physical capacity planning. The ues of Quota reduces the need for policies like *Dominant Resource Fairness*

### Naming and monitoring

To enable service discovery, Borg create a stable "Borg name service" name for each task that includes the cell name, job name and task number. Borg also write tasks' hostname, port job size and task health into Chubby for load balancers.

>Almost every task run under Borg contains a built-in HTTP server that publishes information about the health of
the task and thousands of performance metrics (e.g., RPC latencies).

>A service called Sigma provides a web-based user interface (UI) through which a user can examine the state of all
their jobs, a particular cell, or drill down to individual jobs and tasks to examine their resource behavior, detailed logs,
execution history, and eventual fate. 





