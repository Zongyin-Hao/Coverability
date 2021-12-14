# Coverability  
&ensp; &ensp; 通过反向展开验证1-safe Petri net的可覆盖性, 包含与正向展开的对比  
## 工程结构  
* benchmarks包含论文中介绍的测试样例，样例格式在目录下有说明  
* output是算法的运行结果，当然算法在命令行里也有相应的输出，主要是为了方便做表格/图像，格式在目录下有说明  
* src  
  * main  
    * Main：负责从benchmarks中读数据，启动正向展开算法或反向展开算法，输出并验证结果
    （check函数会在原网上验证输出的轨迹是否真的能使目标标识可覆盖），在output生成相应的文件。
    * PetriNetReader：从文件中读取并生成Petri网  
  * petrinet:Place和Transition  
  * reverseunfolding：反向展开，算法的设计思想和实现细节做了详细注释 
    * roccurrentnet：条件和事件  
    * ReverseUnfolder：相当于一个对外接口，负责网的预处理以及启动反向展开算法，并处理输出结果  
    * ReverseUnfoldingAlgorithm：反向展开算法  
  * unfolding：正向展开，实现细节与反向展开类似，所以有些相同的地方没加注释，建议先阅读反向展开  
  * test：写论文用的，主要是为了自己画个网看看展开长什么样,忽略
  
## 写在最后 
&ensp; &ensp; 这项研究是在Parosh论文的基础上做的，我们发现了Parosh理论的漏洞并做了改进.  
&ensp; &ensp; 目前算法还停留在理论研究阶段, 需进一步
