# cljsc2 [English](README.md)
`cljsc2`可以使用clojure来和星际争霸2游戏进行交互。

它为每个星际2暴露出来的接口生成 clojure.spec specifications 然后使用 protobuf 和 websocket 来运行客户端。

文档:
https://bbss.github.io/cljsc2/

## 安装并运行

如果没玩过星际2或者没怎么用过 Clojure 但是想耍耍这个项目，别客气，作者乐意帮你而且欢迎你发邮件到 baruchberger@gmail.com 。

- 安装 Clojure (比如使用 [leiningen](https://leiningen.org/))

- [安装星际2](http://sc2.blizzard.cn)

文档:
https://bbss.github.io/cljsc2/

想让代码跑起来需要写个Agent,这个方法:
 - 第一个参数接收游戏状态。
 - 第二个参数接收一个运行游戏客户端的链接。
 - 返回一个或多个action来让游戏继续往下走。
 - 返回`nil`结束游戏。

API给机器学习暴露的特征层接口可以用ClojureScript展示到canvas上，参照 [这里](https://github.com/bbss/cljsc2/blob/master/src/cljsc2/cljs/core.cljs)。


## Note
这个项目还挺嫩的, 有bug就跟作者提。所以暂时也没准备好提交到maven仓库里。

## License

Copyright © 2018

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
