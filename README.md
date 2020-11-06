# Coverability  
目的：使用反向展开验证1-safe Petri net的可覆盖性，并与正向展开做对比  
  
工程结构：  
--benchmarks包含论文中介绍的测试样例，样例格式在目录下有说明  
--output是算法的运行结果，当然算法在命令行里也有相应的输出，output里的数据主要是为了做表格，图像方便，格式在目录下有说明  
--src  
----main  
--------Main主类：包括main函数，负责从benchmarks中读数据，启动正向展开算法或反向展开算法，输出并验证结果（check函数会在原网上验证输出的轨迹是否  
真的能使目标标识可覆盖），在output生成相应的文件。具体格式参考源码，还是比较好懂的:-)  
--------PetriNetReader：从文件中读取并生成Petri网  
----petrinet:Place和Transition  
----reverseunfolding：反向展开，里面的类我们都给了详细注释，包括我们的设计思想和实现细节  
--------roccurrentnet：条件和事件  
--------ReverseUnfolder：相当于一个对外接口，负责网的预处理以及启动反向展开算法，并处理输出结果  
--------ReverseUnfoldingAlgorithm：反向展开算法  
----unfolding：正向展开，实现细节与反向展开类似，所以有些相同的地方没加注释，建议先阅读反向展开  
----test：写论文用的，主要是为了自己画个网看看展开长什么样,忽略  
论文后面也会传上来的，如果没被拒稿的话:-(  
  
写在最后：  
这项研究是在Parosh论文的基础上做的，我们发现了Parosh理论的漏洞并做了改进，但不能完全保证我们的理论就没有bug，而且目前我们的算法仍然是比较低效的，  
还需要继续优化。  
如果你发现了我们理论上的问题 or 有更好的idea or 发现了代码bug or 有更好的实现方式 or 有地方难以理解，欢迎与我们交流讨论！  
  
QQ：2987048728  
微信：haozongyin  
邮箱：2987048728@qq.com  
     haozongyin@gmail.com  
(记得简单备注一下)  
