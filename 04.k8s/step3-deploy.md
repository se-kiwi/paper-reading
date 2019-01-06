# 部署应用

## 数据库部署

- ### Mysql：

  需要注意指定MySQL版本以及Root密码。

```yaml
apiVersion: v1
kind: ReplicationController
metadata:
  name: mysql
spec:
  replicas: 1
  selector:
    app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:5.7 #指定版本
          ports:
          - containerPort: 3306
          env:
          - name: MYSQL_ROOT_PASSWORD
            value: "root" #root用户密码
```

- ### MongoDB

  同样需要指定版本：

```yaml
apiVersion: v1
kind: ReplicationController
metadata:
  name: mongodb
spec:
  replicas: 1
  selector:
    app: mongodb
  template:
    metadata:
      labels:
        app: mongodb
    spec:
      containers:
        - name: mongodb
          image: mongo:3.6
          ports:
          - containerPort: 27017
```

为了确保后端能够连接到响应的pod,需要为两个数据库建立Service:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql
spec:
  ports:
    - port: 3306 #容器端口
      target: 3306   #集群内访问端口
  selector:
    app: mysql #需与前边定义的pod label相同
```

## 后端部署

​	同样，后端也通过一个RC进行部署：

```yaml
apiVersion: v1
kind: ReplicationController
metadata:
  name: webkiwi
spec:
  replicas: 1 #副本数
  selector:
    app: webkiwi
  template:
    metadata:
      labels:
        app: webkiwi
    spec:
      containers:
        - name: webkiwi
          image: bywind/flawsweeper-backend #你自己的docker hub中的镜像
          ports:
          - containerPort: 8080
```

​	为了使前端能够访问到后端，也需要配置与之前数据库类似的Service.

## 前端部署

​	前端模块也应通过RC进行部署，与后端部署基本类似，但是Service稍有不同。由于前端需要暴露给用户进行访问，因此需要在Service中将，端口暴露向外部：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: forntendkiwi
spec:
  type: NodePort #使用该模式表示将端口映射到Node上的真实端口
  ports:
    - port: 8080
      targetport: 8080
      nodePort: 30007 #外部端口
  selector:
    app: forntendkiwi
```

