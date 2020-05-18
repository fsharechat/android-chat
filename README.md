[![Build Status](https://travis-ci.org/comsince/universe_push.svg?branch=master)](https://travis-ci.org/comsince/universe_push)
[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/comsince/universe_push/blob/master/LICENCE)
[![Gitee stars](https://gitee.com/comsince/universe_push/badge/star.svg?theme=white)](https://gitee.com/comsince/universe_push)
[![GitHub stars](https://img.shields.io/github/stars/comsince/universe_push?style=social)](https://github.com/comsince/universe_push)

# 飞享

![image](http://image.comsince.cn/fx-chat.png)

**NOTE:** 本项目为`飞享`即时通讯系统的Android客户端

## 适合快速部署的聊天系统
这是一个聊天系统的简单架构，解决大量用户需要即时通讯的解决方案，基于RPC框架Dubbo,SpringBoot构建微服务应用，提供Docker快速部署的解决方案。
提供Android客户端,Web客户端类似微信功能，包括`好友添加`，`私聊`，`群聊`，`一对一音视频通话`等基本功能

### 架构图

**NOTE:** 基本架构图说明为了帮助大家更快了解系统依赖的组件,方便大家快速本地部署

![image](attachment/push-universe.png)

### Android 客户端
[android-chat](https://github.com/comsince/android-chat)客户端，基于java nio实现TCP长连接，使用自定义的二进制协议，全新的心跳设计，实现与web端消息同步，音视频通话等功能。

![image](http://image.comsince.cn/chat-show.gif)

> __<font color="#dd0000">扫码体验APK下载</font>__

![image](http://image.comsince.cn/qr-chat.png)

**NOTE:** 本apk基于[android-chat](https://github.com/comsince/android-chat)构建替换为java协议栈开发

* 请选择其中任何一个帐号密码进行登录即可
```properties
帐号：13800000000, 13800000001, 13800000002
验证码：556677
```

* 技术咨询  
  
当你登录上面其中一个测试帐号后，如果你有什么问题，请到通讯录找到`官方技术支持`发消息即可，你也可以在`IM即时通讯交流组`提问，如果在研究过程中有问题，可以随时咨询，本人尽量提供技术支持，但并不保证随时在线，请尽量在工作日时间发消息

* 官方QQ群交流

![image](http://image.comsince.cn/1-VYVLVL22-1587711095978-/storage/emulated/0/Tencent/QQ_Images/qrcode_1587711062833.jpg)

### 基于Vue的Web客户端
web客户端基于websocket通信，在此协议之上使用json格式的协议进行通讯，详情参见[vue-chat](https://github.com/comsince/vue-chat),支持文本，图片类型发送，支持实时音视频，支持音视频与[android-chat](https://github.com/comsince/android-chat)客户端互通


* 项目预览  

![image](https://user-gold-cdn.xitu.io/2020/4/23/171a4cad1136926f?w=2162&h=1286&f=png&s=487836)

* 语音通话

![image](https://user-gold-cdn.xitu.io/2020/3/20/170f70e65d19d2ac?w=2880&h=1800&f=png&s=1120425)

* 视频通话

![image](https://user-gold-cdn.xitu.io/2020/3/20/170f70e73e8ad91e?w=2880&h=1800&f=png&s=1323835)

**NOTE:** [演示地址](http://www.comsince.cn/chat/index.html) 演示`账号`,`验证码`同手机版一致.由于这里使用的是http服务，chrome浏览器对非https的权限管控，建议使用edge浏览器测试音视频通话，当然你也可以本地部署

#### Vue-Chat公测地址

* 公测地址[chat.comsince.cn](https://chat.comsince.cn)
**NOTE:** 注意`公测地址`与`演示地址`两套环境是独立的，数据不互通，公测环境请使用手机号+验证码登录，如需技术支持或意见反馈请加好友`13900000001`官方技术支持,公测地址带宽只有1M,首次加载可能需要时间比较长，请耐心等待

## 服务说明
聊天系统为了适应大规模用户的链接请求，将服务分为`链接服务`和`消息服务`，它们都是独立的，可以单独部署也可以集群部署
### 链接服务[push-connector]
用于解决用户的链接请求，支撑百万级用户的链接，可单机部署，可集群部署。如果你存在大规模用户链接，可以启动集群模式,参考[K8s自动伸缩模式](#k8s_deployment)
### 消息服务[push-group]
用于用户处理用户管理，会话管理，离线消息处理，群组管理等功能，是整个即时通讯系统的业务处理模块

## 自动化构建
增加持续集成的好处
* 随时随地发布软件
* 任何一次构建都能触发一次发布
* 只需发布一次artifact,即可随时发布

**NOTE:** 以下是发布持续交付工作流图

![image](https://cloud.githubusercontent.com/assets/6069066/14159789/0dd7a7ce-f6e9-11e5-9fbb-a7fe0f4431e3.png)

## 如何启动服务
本机部署只需要两个`SpringBoot`服务，一个`Mysql`服务，一个`zookeeper`服务,链接服务`push-connector`集群模式还需要`kafka`支持

### 部署前准备
* 安装`docker`与`docker-composer`,如果需要在k8s中部署，请准备好相关的环境
* 确保编译此项目`mvn clean package -Dmaven.test.skip=true`

### 生产模式
这种模式下，所有的镜像都会从Docker Hub下载，只需要复制`docker-compose.yml`,在该目录下执行`docker-compose up`即可.
如果要查看完整的部署步骤，请参考这里[基于Docker的即时通讯系统的持续集成发布说明](https://www.comsince.cn/2019/08/07/docker-continuous/#%E4%BB%8Edocker-hub%E4%B8%8B%E8%BD%BD%E9%95%9C%E5%83%8F%E5%8F%91%E5%B8%83im%E7%B3%BB%E7%BB%9F)

### 开发模式
如果你希望自己编译镜像，你必须克隆此代码，并在本地编译此项目。然后执行`docker-compose -f docker-compose.yml -f docker-compose-dev.yml up`  

### K8S中部署<a name="k8s_deployment"/>
如果想在k8s中部署，我们也提供yml配置，执行以下命令即可，详情参考[即时通讯服务在k8s容器的部署说明](https://www.comsince.cn/2019/08/12/universe-kubenetes-intro/)
```shell
kubectl apply -f https://www.comsince.cn/download/cloud-native/universe-kube-deployment.yml
```
或者下载代码执行,`push-connector`支持扩展，以适应海量长连接，集群模式需要`kafka`支持，如果kafka没有启动成功，可以手动重启`push-connector`
```shell
kubectl apply -f ./universe-kube-deployment.yml
```

![image](http://image.comsince.cn/push-connector-scale.png)

## 本地开发测试
**NOTE:** 准备好mysql与zookeeper服务，配置好相应的配置，直接启动`push-connector`和`push-group`两个spring-boot服务即可

## 虚拟机部署
**NOTE:** 如果你希望直接脚本部署，[参考脚本部署](README-Linux.md),例如如下是`push-connector`服务

* 启动`spring-boot-dubbo-push-connector`链接网关服务
```shell
/opt/boot/push-api
├── jvm.ini
├── push-connector //可执行启动脚本
└── log
   └── push-connector.log //存放日志
└── lib
   └── spring-boot-dubbo-push-connector-1.0.0-SNAPSHOT.jar //可运行的jar
```

* 执行启动服务

```shell
# 启动服务
./push-connector start
# 停止服务
./push-connector stop
```

## CentOS部署

**NOTE:** 如果你在部署中遇到问题，可以到上面演示环境找到`官方技术支持`寻求解答，也可以到QQ群需求帮助，如果你有在其他环境成功安装的经验，欢迎反馈给我们

* [即时聊天系统在Centos上单机部署实践](https://www.comsince.cn/2020/04/13/universe-push-start-on-centos/)

## Windows部署

* [即时聊天系统在Windows上单机测试部署实践](https://www.comsince.cn/2020/05/07/universe-push-start-on-windows/)

## 欢迎为此项目作出贡献
该项目是开源项目，欢迎提出建议或者提供意见反馈，如果你喜欢此项目，请点击`star`支持我们不断改进

## 感谢
此项目时在参考其他项目基础上完成，在此表示感谢
* [t-io](https://github.com/tywo45/t-io)
* [wildfirechat](https://gitee.com/wildfirechat/server)

## 一次性赞助

是采用 MIT 许可的开源项目，个人使用完全免费。但是随着项目的增长，也需要相应的资金支持，你可以通过以下方式来赞助此项目

| 支付宝      | 微信| 
| :--------: | :--------:| 
|<img src="http://image.comsince.cn/zfb-purse.png" alt="图片替换文本" width="300" height="300" align="center" />|<img src="http://image.comsince.cn/wx-purse.png" alt="图片替换文本" width="300" height="300" align="center" />|

## 永久授权

如果公司采用本项目，需要二次开发，可以申请软件授权，收取一次性费用，可以提供6个月的技术支持，联系QQ：`1282212195`
